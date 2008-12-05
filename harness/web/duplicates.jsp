<%@ page language="java" import="com.sun.faban.harness.webclient.ResultAction,
                                 java.util.HashSet,
                                 com.sun.faban.harness.webclient.Result"
 %>
 <%  HashSet<String> duplicateSet = (HashSet<String>) request.getAttribute("duplicates");
     for (String runId : duplicateSet){
         out.println(runId); 
     }
 %>
