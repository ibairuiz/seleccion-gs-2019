<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %><%@
taglib uri="http://liferay.com/tld/clay" prefix="clay" %><%@
taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %><%@
taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %><%@
taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="com.liferay.petra.string.StringPool" %><%@
page import="com.liferay.portal.kernel.util.Constants" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />

<%
	String csvSeparator = portletPreferences.getValue("csvSeparator", StringPool.BLANK);
	boolean isDataFilePathChangeable = Boolean.valueOf(portletPreferences.getValue("isDataFilePathChangeable", Boolean.FALSE.toString()));
	String dataRootDir = portletPreferences.getValue("dataRootDir", StringPool.BLANK);
	String emailFromAddress = portletPreferences.getValue("emailFromAddress", StringPool.BLANK);
	String emailFromName = portletPreferences.getValue("emailFromName", StringPool.BLANK);
	boolean isValidationScriptEnabled = Boolean.valueOf(portletPreferences.getValue("isValidationScriptEnabled", Boolean.FALSE.toString()));
%>