<%@ page language="java" import="java.util.HashSet"
 %>
 <%  HashSet<String> duplicateSet = (HashSet<String>) request.getAttribute("duplicates");
     for (String runId : duplicateSet){
         out.println(runId); 
     }
 %>
