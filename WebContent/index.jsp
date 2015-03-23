<%@page import="session_management.SSMServlet"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8">
    <title>Project 1a</title>
    
    <script type="text/javascript">
    	window.onload = function() {
    		getCookie("CS5300PROJ1SESSION");
    	}
    
	    function getCookie(cname) {
	        var name = cname + "=";
	        var ca = document.cookie.split(';');
	        for(var i=0; i<ca.length; i++) {
	            var c = ca[i];
	            while (c.charAt(0)==' ') c = c.substring(1);
	            if (c.indexOf(name) == 0) {
	            	var value = c.substring(name.length,c.length);
	            	document.getElementById('sessionId').innerHTML = value.split('_')[0];
	            	document.getElementById('version').innerHTML = value.split('_')[1];
	            	document.getElementById('primary').innerHTML = value.split('_')[2];
	            	document.getElementById('backup').innerHTML = value.split('_')[3];
	            	return value;
	            }
	        }
	        return "";
	    }
    </script>
</head>
<body>
    <div id='content' class="container">
        <h1>${message}</h1>
        <form method='POST' action="">
        	<div class='row'>
        		<input type='submit' name='btn-submit' value='Replace'>
        		<input type='text' id='newMessage' name='newMessage' maxLength='256'>
        	</div>
        	<div class='row'>
        		<input type='submit' name='btn-submit' value='Refresh'>
        	</div>
        	<div class='row'>
        		<input type='submit' name='btn-submit' value='Logout'>
        	</div>
        	<div class='row'>
	        	<dl>
    	    		<dt>Session ID</dt>
        			<dd id='sessionId'></dd>
        			<dt>Version No.</dt>
        			<dd id='version'></dd>
	        		<dt>Expires on</dt>
    	    		<dd>${expiresOn}</dd>
    	    		<dt>Primary</dt>
    	    		<dd id='primary'></dd>
    	    		<dt>Backup</dt>
    	    		<dd id='backup'></dd>
        		</dl>
        	</div>
        </form>
        <h2>Server View</h2>
        <table>
        	<tr>
        		<th>Address</th>
        		<th>Status</th>
        		<th>Last Seen</th>
        	</tr>
        <%
        	String server_view = SSMServlet.serverViewTable.toString();
        	String[] view_entries = server_view.split(java.util.regex.Pattern.quote(","));
        	for(String entry : view_entries){
        		String address = entry.split(java.util.regex.Pattern.quote(">"))[0];
        		String status = entry.split(java.util.regex.Pattern.quote(">"))[1].split(java.util.regex.Pattern.quote("+"))[0];
        		if(status.equals("1")) status = "UP";
        		else status = "DOWN";
        		String lastSeen = entry.split(java.util.regex.Pattern.quote(">"))[1].split(java.util.regex.Pattern.quote("+"))[1];
        %>
        	<tr>
        		<td><%=address %></td>
        		<td><%=status %></td>
        		<td><%=lastSeen %></td>
        	</tr>
        <%
        	}
        %>
        </table>
    </div>
</body>
</html>