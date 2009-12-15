/* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://www.sun.com/cddl/cddl.html or
* install_dir/legal/LICENSE
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at install_dir/legal/LICENSE.
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
* Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
*/

package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.FileHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class maintains the tags to runs mapping.
 *
 * @author Sheetal Patil
 */
public class TagEngine implements Serializable{

    private static final long serialVersionUID = 20090302L;

    private static Logger logger = Logger.getLogger(TagEngine.class.getName());
    private static TagEngine instance = null;

    
    HashMap<String, Entry> tagEntries = new HashMap<String, Entry>();
    HashMap<String, HashSet<Entry>> runEntries =
                                    new HashMap<String, HashSet<Entry>>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock wlock = rwl.writeLock();
    private final Lock rlock = rwl.readLock();

    /**
     * Creates the singleton instance of the tag engine, reads it from the
     * serialized tag engine if available. Otherwise it will re-index.
     * @return The tag engine
     * @throws java.io.IOException Error reading from the file
     * @throws java.lang.ClassNotFoundException Error loading TagEngine class
     */
    private static TagEngine createInstance()
            throws IOException, ClassNotFoundException {
        // 1. Check for existence of ser file
        // 2. If exists, load ser file
        // 3. If not exists, scan and create tag engine
        // 4. Save output to ser file
        File serFile = new File(Config.CONFIG_DIR + "/tagengine.ser");
        if (serFile.exists()) {
            try {
            ObjectInputStream in = new ObjectInputStream(
                                        new FileInputStream(serFile));
            instance = (TagEngine) in.readObject();
            in.close();
            } catch (InvalidClassException e) {
                logger.log(Level.WARNING,
                        "Old tagengine.ser may be incompatible, ignoring.", e);
            }
        }

        if (instance == null) {
            instance = new TagEngine();
            File[] dirs = new File(Config.OUT_DIR).listFiles();
            ArrayList<String> runIdList = new ArrayList<String>(dirs.length);

            for (File runDir : dirs) {
                if (!runDir.isDirectory())
                    continue;
                String runIdS = runDir.getName();
                runIdList.add(runIdS);
            }
            instance.wlock.lock();
            try {
                for (String runId : runIdList){
                    File file = new File(Config.OUT_DIR + runId +
                                                        "/META-INF/tags");
                    String[] tags = FileHelper.readArrayContentFromFile(file);
                    if (tags != null && tags.length > 0) {
                        instance.add(runId, tags);
                    }
                }
            }finally {
                instance.wlock.unlock();
            }
            instance.save();
        }
        return instance;
    }

    /**
     * Obtains the singleton instance of the tag engine.
     * @return The singleton instance of the tag engine
     * @throws java.io.IOException Error reading tag information
     * @throws java.lang.ClassNotFoundException Error creating the singleton
     */
    public static TagEngine getInstance()
            throws IOException, ClassNotFoundException {
        // There should not be concurrency on first access at all.
        // We know, double-checks are not absolutely safe, but are by far safe
        // enough for the task at hand. We also do not want to pay the
        // synchronize overhead for every getInstance call.
        if (instance == null) {
            synchronized (TagEngine.class) {
                if (instance == null)
                    instance = createInstance();
            }
        }            
        return instance;
    }

    /**
     * Private constructor. Nobody should ever construct TagEngine.
     */
    private TagEngine() {
    }

    /**
     * Serializes the tag engine to file. It is located at
     * $FABAN/config/tagengine.ser.
     */
    public void save() {
       File filename = new File(Config.CONFIG_DIR + "/tagengine.ser");
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(filename));
            out.writeObject(this);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error saving tag engine.", e);
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing tag engine file " +
                            filename, e);
                }
        }
    }

    /**
     * Searches the tag engine for runs matching the given tags. The tags
     * are given as a string separated by space, comma, colon, or semicolon.
     * @param tags The tag in question, '/' seperated from sub-tags
     * @return The set of run ids matching the given tags
     */
    public Set<String> search(String tags) {
        String[] tagsArray = null;
        if (tags != null && !"".equals(tags)) {
            StringTokenizer tok = new StringTokenizer(tags, " ,:;");
            tagsArray = new String[tok.countTokens()];
            int count = tok.countTokens();
            int i = 0;
            while (i < count) {
                String nextT = tok.nextToken().trim();
                tagsArray[i] = nextT;
                i++;
            }
        }
        return search(tagsArray);
    }

    /**
     * Searches the tag engine for runs matching the given tags given
     * an array of tags.
     * @param tags The tags in question, '/' seperated from sub-tags
     * @return The set of run ids matching the given tags
     */
    public Set<String> search(String[] tags) {

        HashSet<String> finalAnswer = new HashSet<String>();
        if (tags != null) {
            int count = 0;
            rlock.lock();
            try {
                for (String tag : tags) {
                    // In many instances, the separator is URL encoded from '/'
                    // to "+", so we have to change them back.
                    tag = tag.replace("+", "/");
                    HashSet<String> answer = new HashSet<String>();
                    Entry entry = findEntry(tag);
                    if (entry != null) {

                        // Flatten the entry hierarchy into the ArrayList entries.
                        ArrayList<Entry> entries = new ArrayList<Entry>();
                        entries.addAll(entry.subtags.values());

                        // Keep flattening down the hierarchy
                        // entries.size keeps increasing as we go down
                        // the hierarchy. That's why we need to keep
                        // evaluating entries.size() and cannot use an iterator.
                        for (int i = 0; i < entries.size(); i++) {
                            Entry subEntry = entries.get(i);
                            entries.addAll(subEntry.subtags.values());
                        }

                        // Adds run ids from the flattened entries to the answer.
                        answer.addAll(entry.runIds);
                        for (Entry subEntry : entries) {
                            answer.addAll(subEntry.runIds);
                        }
                        if (count == 0) {
                            finalAnswer = answer;
                        }
                    }
                    finalAnswer.retainAll(answer);
                    answer = null;
                    count++;
                }
            } finally {
                rlock.unlock();
            }
        } 
        return finalAnswer;
    }
    
    private Entry findEntry(String tag) {
        tag = tag.toLowerCase();
        StringTokenizer t = new StringTokenizer(tag, "/");
        String topTag = t.nextToken();
        Entry entry = tagEntries.get(topTag);          
        String subTag = topTag;
        // Keep finding the entry that represents the exact tag/subtag.
        while(entry != null && t.hasMoreTokens()) {
            subTag = subTag + "/" + t.nextToken();
            entry = entry.subtags.get(subTag);
            if (entry == null)
                break;
        }
        return entry;
    }

    /**
     * Removes a run from the tag engine.
     * @param runId The id of the run
     */
    public void removeRun(String runId) {
        wlock.lock();
        try {
            HashSet<Entry> removeSet = runEntries.get(runId);
            for (Entry entry : removeSet) {
                entry.runIds.remove(runId);
            }
            runEntries.remove(runId);
        } finally {
            wlock.unlock();
        }
    }

    private String[] getTagsArray(String tag) {
        String[] tagsArray = null;
        if (tag != null && !"".equals(tag)) {
            StringTokenizer tok = new StringTokenizer(tag, "/");
            tagsArray = new String[tok.countTokens()];
            int count = tok.countTokens();
            int i = 0;
            while (i < count) {
                String nextT = tok.nextToken().trim();
                if (i == 0) {
                    tagsArray[i] = nextT;
                } else {
                    tagsArray[i] = tagsArray[i - 1] + "/" + nextT;
                }
                i++;
            }
        }
        return tagsArray;
    }

    /**
     * Adds a set of tags for a run id to the tag engine.
     * Old tags for the run id will be replaced with the new one.
     * If tags is null or an empty array, all tags will be removed for the
     * given run id.
     * @param runId The runId
     * @param tags The list of tags to add
     */
    public void add(String runId, String[] tags) {
        wlock.lock();
        try {
            HashSet<Entry> entrySet = runEntries.get(runId);
            if(entrySet == null)
                entrySet = new HashSet<Entry>();
            HashSet<Entry> newEntrySet = new HashSet<Entry>();
            for (String tag : tags) {
                tag = tag.toLowerCase();
                String[] tagsArray = getTagsArray(tag);
                Entry entry = null;
                int count = 0;
                for(String subtag : tagsArray){
                    if (count == 0) {
                        entry = findEntry(subtag);
                        if (entry == null) {
                            entry = new Entry();
                            entry.fullTagName = subtag;
                            tagEntries.put(subtag, entry);
                        }
                    }else{
                        Entry subEntry = findEntry(subtag);
                        if (subEntry == null) {
                            subEntry  = new Entry();
                            subEntry.fullTagName = subtag;
                            tagEntries.put(subtag, subEntry);
                            entry.subtags.put(subtag, subEntry);
                        }
                    }
                    entry = tagEntries.get(subtag);
                    count++;
                }
                entry.runIds.add(runId);
                tagEntries.put(tag, entry);
                newEntrySet.add(entry);
            }

            HashSet<Entry> removeEntrySet = new HashSet<Entry>(entrySet);
            removeEntrySet.removeAll(newEntrySet);

            for (Entry entry : removeEntrySet) {
                entry.runIds.remove(runId);
                if (entry.runIds.size() == 0 && entry.subtags.size() == 0) {
                    //removeEntry(entry);
                    entry.subtags.remove(entry);
                }
            }
            entrySet.removeAll(removeEntrySet);
            entrySet.addAll(newEntrySet);
            runEntries.put(runId, entrySet);
        } finally {
            wlock.unlock();
        }
    }

    /**
     * Represents a chainable entry in the tag engine.
     */
    static class Entry implements Serializable{
        String fullTagName;
        HashSet<String> runIds = new HashSet<String>();
        HashMap<String, Entry> subtags = new HashMap<String, Entry>();
    }
}