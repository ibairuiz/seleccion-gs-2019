package mvc.portlet.configuration;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.portlet.ConfigurationAction;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.util.ParamUtil;

import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mvc.portlet.constants.FormPortletKeys;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Liferay
 */
@Component(
	configurationPid = FormPortletKeys.MVC_PORTLET_CONFIGURATION_PID,
	configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true,
	property = "javax.portlet.name=" + FormPortletKeys.MVC_PORTLET_NAME,
	service = ConfigurationAction.class
)
public class FormPortletConfigurationAction extends DefaultConfigurationAction {

	@Override
	public void include(
			PortletConfig portletConfig, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws Exception {

		httpServletRequest.setAttribute(
			FormPortletConfiguration.class.getName(),
			_formPortletConfiguration);

		super.include(portletConfig, httpServletRequest, httpServletResponse);
	}

	@Override
	public void processAction(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse)
		throws Exception {

		String emailFromAddress = ParamUtil.getString(
			actionRequest, "emailFromAddress");
		String csvSeparator = ParamUtil.getString(
			actionRequest, "csvSeparator");
		String dataRootDir = ParamUtil.getString(actionRequest, "dataRootDir");
		String emailFromName = ParamUtil.getString(
			actionRequest, "emailFromName");
		String isDataFilePathChangeable = ParamUtil.getString(
			actionRequest, "isDataFilePathChangeable");
		String isValidationScriptEnabled = ParamUtil.getString(
			actionRequest, "isValidationScriptEnabled");

		setPreference(actionRequest, "csvSeparator", csvSeparator);
		setPreference(actionRequest, "dataRootDir", dataRootDir);
		setPreference(actionRequest, "emailFromAddress", emailFromAddress);
		setPreference(actionRequest, "emailFromName", emailFromName);
		setPreference(
			actionRequest, "isDataFilePathChangeable",
			isDataFilePathChangeable);
		setPreference(
			actionRequest, "isValidationScriptEnabled",
			isValidationScriptEnabled);
		super.processAction(portletConfig, actionRequest, actionResponse);
	}

	@Activate
	@Modified
	protected void activate(Map<Object, Object> properties) {
		_formPortletConfiguration = ConfigurableUtil.createConfigurable(
			FormPortletConfiguration.class, properties);
	}

	private volatile FormPortletConfiguration _formPortletConfiguration;

}