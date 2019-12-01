<%@ include file="/init.jsp" %>

<liferay-portlet:actionURL
	var="sendActionURL"
/>

<aui:form action="<%= sendActionURL %>" method="post" name="fm">
	<aui:input
		name="<%= Constants.CMD %>"
		type="hidden"
		value="<%= Constants.SAVE %>"
	/>

	<aui:fieldset>
		<c:forEach items="${journalArticleURLTitleList}" var="urlTitle" varStatus="loop">
			<h1>URL: ${urlTitle}</h1>

			<c:set value="field${loop.index}" var="field" />

			<aui:input name="${field}" placeholder="${urlTitle}" type="text" />
		</c:forEach>
	</aui:fieldset>

	<aui:button type="submit">Save</aui:button>
</aui:form>