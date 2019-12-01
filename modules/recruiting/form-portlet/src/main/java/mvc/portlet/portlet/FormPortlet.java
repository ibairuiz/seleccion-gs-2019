package mvc.portlet.portlet;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailServiceUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import mvc.portlet.configuration.FormPortletConfiguration;
import mvc.portlet.constants.FormPortletKeys;
import mvc.portlet.util.FormUtil;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author ibairuiz
 */
@Component(
	configurationPid = FormPortletKeys.MVC_PORTLET_CONFIGURATION_PID,
	immediate = true,
	property = {
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.init-param.config-template=/configuration.jsp",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.display-name=My MVC PORTLET",
		"javax.portlet.name=" + FormPortletKeys.MVC_PORTLET_NAME,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user"
	},
	service = Portlet.class
)
public class FormPortlet extends MVCPortlet {

	@Override
	public void doView(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		List<JournalArticle> journalArticleList =
			_journalArticleLocalService.getArticles();

		renderRequest.setAttribute(
			"journalArticleURLTitleList",
			journalArticleList.stream(
			).map(
				JournalArticle::getUrlTitle
			).collect(
				Collectors.toList()
			));

		super.doView(renderRequest, renderResponse);
	}

	@Override
	public void processAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws IOException, PortletException {

		String command = ParamUtil.getString(actionRequest, Constants.CMD);

		long defaultUserId = 0;

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (null == serviceContext) {
			long companyId = PortalUtil.getDefaultCompanyId();

			try {
				defaultUserId = UserLocalServiceUtil.getDefaultUserId(
					companyId);

				_log.debug(defaultUserId);
			}
			catch (Exception e) {
				_log.error(e);
			}
		}

		if (FormPortletKeys.CMD_SAVE.equals(command) && (defaultUserId == 0)) {
			try {
				saveData(actionRequest, actionResponse);
			}
			catch (Exception e) {
				_log.error(e);
			}
		}

		super.processAction(actionRequest, actionResponse);
	}

	public void saveData(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		String portletId = PortalUtil.getPortletId(actionRequest);

		PortletPreferences preferences =
			PortletPreferencesFactoryUtil.getPortletSetup(
				actionRequest, portletId);

		String successURL = GetterUtil.getString(
			preferences.getValue("successURL", StringPool.BLANK));
		boolean sendAsEmail = GetterUtil.getBoolean(
			preferences.getValue("sendAsEmail", StringPool.BLANK));
		boolean saveToDatabase = GetterUtil.getBoolean(
			preferences.getValue("saveToDatabase", StringPool.BLANK));
		String databaseTableName = GetterUtil.getString(
			preferences.getValue("databaseTableName", StringPool.BLANK));
		boolean saveToFile = GetterUtil.getBoolean(
			preferences.getValue("saveToFile", StringPool.BLANK));
		String fileName = GetterUtil.getString(
			preferences.getValue("fileName", StringPool.BLANK));

		Map<String, String> f = new LinkedHashMap<>();

		for (int i = 1; true; i++) {
			String fieldLabel = preferences.getValue(
				"fieldLabel" + i, StringPool.BLANK);

			if (Validator.isNull(fieldLabel)) {
				break;
			}

			String fieldType = preferences.getValue(
				"fieldType" + i, StringPool.BLANK);

			if (StringUtil.equalsIgnoreCase(fieldType, "paragraph")) {
				continue;
			}

			f.put(fieldLabel, ParamUtil.getString(actionRequest, "field" + i));
		}

		PortletSession portletSession = actionRequest.getPortletSession();

		portletSession.setAttribute(
			_SAVED_DATA_CACHE + System.currentTimeMillis(), f);

		Set<String> e = null;

		try {
			e = validate(f, preferences);
		}
		catch (Exception ex) {
			SessionErrors.add(
				actionRequest, "validationScriptError", ex.getMessage());

			return;
		}

		if (e.isEmpty()) {
			boolean emailSuccess = true;
			boolean databaseSuccess = true;
			boolean fileSuccess = true;

			ThemeDisplay themeDisplay =
				(ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

			if (sendAsEmail) {
				emailSuccess = sendEmail(
					themeDisplay.getCompanyId(), f, preferences);
			}

			if (saveToDatabase) {
				if (Validator.isNull(databaseTableName)) {
					databaseTableName = FormUtil.getNewDatabaseTableName(
						portletId);

					preferences.setValue(
						"databaseTableName", databaseTableName);

					preferences.store();
				}

				databaseSuccess = saveDatabase(
					themeDisplay.getCompanyId(), f, preferences,
					databaseTableName);
			}

			if (saveToFile) {
				if (!_formPortletConfiguration.isDataFilePathChangeable()) {
					fileName = FormUtil.getFileName(themeDisplay, portletId);
				}

				fileSuccess = saveFile(f, fileName);
			}

			if (emailSuccess && databaseSuccess && fileSuccess) {
				if (Validator.isNull(successURL)) {
					SessionMessages.add(actionRequest, "success");
				}
				else {
					SessionMessages.add(
						actionRequest,
						portletId +
							SessionMessages.
								KEY_SUFFIX_HIDE_DEFAULT_SUCCESS_MESSAGE);
				}
			}
			else {
				SessionErrors.add(actionRequest, "error");
			}
		}
		else {
			for (String bF : e) {
				SessionErrors.add(actionRequest, "error" + bF);
			}
		}

		if (SessionErrors.isEmpty(actionRequest) &&
			Validator.isNotNull(successURL)) {

			actionResponse.sendRedirect(successURL);
		}
	}

	@Activate
	@Modified
	protected void activate(Map<Object, Object> properties) {
		_formPortletConfiguration = ConfigurableUtil.createConfigurable(
			FormPortletConfiguration.class, properties);
	}

	protected String getCSVFormattedValue(String value) {
		StringBundler sb = new StringBundler(3);

		sb.append(CharPool.QUOTE);
		sb.append(
			StringUtil.replace(value, CharPool.QUOTE, StringPool.DOUBLE_QUOTE));
		sb.append(CharPool.QUOTE);

		return sb.toString();
	}

	protected String getMailBody(Map<String, String> fieldsMap) {
		StringBundler mailBodySB = new StringBundler(4);

		for (Map.Entry<String, String> entry : fieldsMap.entrySet()) {
			String fieldValue = entry.getValue();

			mailBodySB.append(entry.getKey());
			mailBodySB.append(" : ");
			mailBodySB.append(fieldValue);
			mailBodySB.append(CharPool.NEW_LINE);
		}

		return mailBodySB.toString();
	}

	protected boolean saveDatabase(
			long companyId, Map<String, String> fieldsMap,
			PortletPreferences preferences, String databaseTableName)
		throws PortalException {

		FormUtil.checkTable(companyId, databaseTableName, preferences);

		long classPK = CounterLocalServiceUtil.increment(
			FormUtil.class.getName());

		for (Map.Entry<String, String> entry : fieldsMap.entrySet()) {
			String fieldValue = entry.getValue();

			ExpandoValueLocalServiceUtil.addValue(
				companyId, FormUtil.class.getName(), databaseTableName,
				entry.getKey(), classPK, fieldValue);
		}

		return true;
	}

	protected boolean saveFile(Map<String, String> fieldsMap, String fileName) {
		StringBundler sb = new StringBundler();

		for (String value : fieldsMap.values()) {
			sb.append(getCSVFormattedValue(value));
			sb.append(_formPortletConfiguration.csvSeparator());
		}

		sb.setIndex(sb.index() - 1);

		sb.append(CharPool.NEW_LINE);

		try {
			FileUtil.write(fileName, sb.toString(), false, true);

			return true;
		}
		catch (IOException ioe) {
			_log.error(ioe);
		}

		return false;
	}

	protected boolean sendEmail(
		long companyId, Map<String, String> fieldsMap,
		PortletPreferences preferences) {

		try {
			String emailAddresses = preferences.getValue(
				"emailAddress", StringPool.BLANK);

			if (Validator.isNull(emailAddresses)) {
				_log.error(
					"The web form email cannot be sent because no email " +
						"address is configured");

				return false;
			}

			InternetAddress fromAddress = new InternetAddress(
				FormUtil.getEmailFromAddress(preferences, companyId),
				FormUtil.getEmailFromName(preferences, companyId));
			String subject = preferences.getValue("subject", StringPool.BLANK);
			String body = getMailBody(fieldsMap);

			MailMessage mailMessage = new MailMessage(
				fromAddress, subject, body, false);

			InternetAddress[] toAddresses = InternetAddress.parse(
				emailAddresses);

			mailMessage.setTo(toAddresses);

			MailServiceUtil.sendEmail(mailMessage);

			return true;
		}
		catch (AddressException | UnsupportedEncodingException e) {
			_log.error(e);
		}

		return false;
	}

	protected Set<String> validate(
		Map<String, String> fieldsMap, PortletPreferences preferences) {

		Set<String> validationErrors = new HashSet<>();

		StringBundler debugMsgSB = new StringBundler();

		for (int i = 0; i < fieldsMap.size(); i++) {
			String fieldType = preferences.getValue(
				"fieldType" + (i + 1), StringPool.BLANK);

			String fieldLabel = preferences.getValue(
				"fieldLabel" + (i + 1), StringPool.BLANK);

			String fieldValue = fieldsMap.get(fieldLabel);

			boolean fieldOptional = GetterUtil.getBoolean(
				preferences.getValue(
					"fieldOptional" + (i + 1), StringPool.BLANK));

			debugMsgSB.append("Validating fieldType ");
			debugMsgSB.append(i + 1);
			debugMsgSB.append(": ");
			debugMsgSB.append(fieldType);
			debugMsgSB.append("Validating fieldLabel ");
			debugMsgSB.append(i + 1);
			debugMsgSB.append(": ");
			debugMsgSB.append(fieldLabel);
			debugMsgSB.append("Validating fieldOptional ");
			debugMsgSB.append(i + 1);
			debugMsgSB.append(": ");
			debugMsgSB.append(fieldOptional);

			if (Objects.equals("paragraph", fieldType)) {
				continue;
			}

			if (!fieldOptional && Validator.isNotNull(fieldLabel) &&
				Validator.isNull(fieldValue)) {

				validationErrors.add(fieldLabel);

				continue;
			}

			if (!_formPortletConfiguration.isValidationScriptEnabled()) {
				continue;
			}

			String validationScript = GetterUtil.getString(
				preferences.getValue(
					"fieldValidationScript" + (i + 1), StringPool.BLANK));

			if (Validator.isNotNull(validationScript) &&
				!FormUtil.validate(fieldValue, fieldsMap, validationScript)) {

				validationErrors.add(fieldLabel);

				debugMsgSB.append(validationErrors);

				continue;
			}

			_log.debug(debugMsgSB);
		}

		return validationErrors;
	}

	private static final String _SAVED_DATA_CACHE = "FORM_SAVED_DATA_CACHE";

	private static Log _log = LogFactoryUtil.getLog(FormPortlet.class);

	private volatile FormPortletConfiguration _formPortletConfiguration;

	@Reference
	private JournalArticleLocalService _journalArticleLocalService;

}