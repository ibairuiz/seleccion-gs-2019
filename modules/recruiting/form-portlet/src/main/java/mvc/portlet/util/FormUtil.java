package mvc.portlet.util;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoRowLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletPreferences;

import mvc.portlet.configuration.FormPortletConfiguration;
import mvc.portlet.constants.FormPortletKeys;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Liferay
 */
@Component(configurationPid = FormPortletKeys.MVC_PORTLET_CONFIGURATION_PID)
public class FormUtil {

	public static ExpandoTable checkTable(
		long companyId, String tableName, PortletPreferences preferences) {

		ExpandoTable expandoTable = null;

		try {
			expandoTable = ExpandoTableLocalServiceUtil.addTable(
				companyId, FormUtil.class.getName(), tableName);

			int i = 1;

			String fieldLabel = preferences.getValue(
				"fieldLabel" + i, StringPool.BLANK);
			String fieldType = preferences.getValue(
				"fieldType" + i, StringPool.BLANK);

			while ((i == 1) || Validator.isNotNull(fieldLabel)) {
				if (!StringUtil.equalsIgnoreCase(fieldType, "paragraph")) {
					ExpandoColumnLocalServiceUtil.addColumn(
						expandoTable.getTableId(), fieldLabel,
						ExpandoColumnConstants.STRING);
				}

				i++;

				fieldLabel = preferences.getValue(
					"fieldLabel" + i, StringPool.BLANK);
				fieldType = preferences.getValue(
					"fieldType" + i, StringPool.BLANK);
			}
		}
		catch (PortalException pe) {
			_log.error(pe);
		}

		return expandoTable;
	}

	public static String getEmailFromAddress(
		PortletPreferences preferences, long companyId) {

		return PortalUtil.getEmailFromAddress(
			preferences, companyId,
			_formPortletConfiguration.emailFromAddress());
	}

	public static String getEmailFromName(
		PortletPreferences preferences, long companyId) {

		return PortalUtil.getEmailFromName(
			preferences, companyId, _formPortletConfiguration.emailFromName());
	}

	public static String getFileName(
		ThemeDisplay themeDisplay, String portletId) {

		StringBundler sb = new StringBundler(8);

		sb.append(_formPortletConfiguration.dataRootDir());
		sb.append(StringPool.FORWARD_SLASH);
		sb.append(themeDisplay.getScopeGroupId());
		sb.append("/");
		sb.append(themeDisplay.getPlid());
		sb.append(StringPool.FORWARD_SLASH);
		sb.append(portletId);
		sb.append(".csv");

		return sb.toString();
	}

	public static String getNewDatabaseTableName(String portletId) {
		long formId = CounterLocalServiceUtil.increment(
			FormUtil.class.getName());

		return portletId + StringPool.UNDERLINE + formId;
	}

	public static int getTableRowsCount(long companyId, String tableName) {
		return ExpandoRowLocalServiceUtil.getRowsCount(
			companyId, FormUtil.class.getName(), tableName);
	}

	public static String[] split(String s) {
		return split(s, StringPool.COMMA);
	}

	public static String[] split(String s, String delimiter) {
		if ((s == null) || (delimiter == null)) {
			return new String[0];
		}

		s = s.trim();

		if (!s.endsWith(delimiter)) {
			s = s.concat(delimiter);
		}

		if (s.equals(delimiter)) {
			return new String[0];
		}

		List<String> nodeValues = new ArrayList<>();

		if (delimiter.equals("\n") || delimiter.equals("\r")) {
			try {
				BufferedReader br = new BufferedReader(new StringReader(s));

				String line = null;

				while ((line = br.readLine()) != null) {
					nodeValues.add(line);
				}

				br.close();
			}
			catch (IOException ioe) {
				_log.error(ioe);
			}
		}
		else {
			int offset = 0;

			int pos = s.indexOf(delimiter, offset);

			while (pos != -1) {
				nodeValues.add(s.substring(offset, pos));

				offset = pos + delimiter.length();
				pos = s.indexOf(delimiter, offset);
			}
		}

		return nodeValues.toArray(new String[0]);
	}

	public static boolean validate(
		String currentFieldValue, Map<String, String> fieldsMap,
		String validationScript) {

		boolean validationResult = false;

		Context cx = Context.enter();

		StringBundler sb = new StringBundler();

		sb.append("currentFieldValue = String('");
		sb.append(HtmlUtil.escapeJS(currentFieldValue));
		sb.append("');\n");

		sb.append("var fieldsMap = {};\n");

		for (Map.Entry<String, String> entry : fieldsMap.entrySet()) {
			sb.append("fieldsMap['");
			sb.append(entry.getKey());
			sb.append("'] = '");
			sb.append(HtmlUtil.escapeJS(entry.getValue()));
			sb.append("';\n");
		}

		sb.append("function validation(currentFieldValue, fieldsMap) {\n");
		sb.append(validationScript);
		sb.append("}\n");
		sb.append(
			"internalValidationResult = validation(currentFieldValue, fieldsMap);");

		String script = sb.toString();

		Scriptable scope = cx.initStandardObjects();

		Object jsFieldsMap = Context.toObject(fieldsMap, scope);

		ScriptableObject.putProperty(scope, "jsFieldsMap", jsFieldsMap);

		cx.evaluateString(scope, script, "Validation Script", 1, null);

		Object obj = ScriptableObject.getProperty(
			scope, "internalValidationResult");

		if (obj instanceof Boolean) {
			validationResult = (Boolean)obj;
		}
		else if (_log.isDebugEnabled()) {
			String msg =
				"The following script has execution errors:\n" +
					validationScript;

			_log.debug(msg);
		}

		Context.exit();

		return validationResult;
	}

	@Activate
	@Modified
	protected void activate(Map<Object, Object> properties) {
		_formPortletConfiguration = ConfigurableUtil.createConfigurable(
			FormPortletConfiguration.class, properties);
	}

	private static Log _log = LogFactoryUtil.getLog(FormUtil.class);

	private static volatile FormPortletConfiguration _formPortletConfiguration;

}