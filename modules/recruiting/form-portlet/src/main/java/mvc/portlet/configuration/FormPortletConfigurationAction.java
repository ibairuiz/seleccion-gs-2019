package mvc.portlet.configuration;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.RenderParameters;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.liferay.portal.kernel.portlet.ConfigurationAction;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;

import mvc.portlet.constants.FormPortletKeys;

/**
 * @author Liferay
 */
@Component(
	configurationPid = FormPortletKeys.MVC_PORTLET_CONFIGURATION_PID,
	configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true,
	property = "javax.portlet.name=" + FormPortletKeys.MVC_PORTLET_NAME,
	service = ConfigurationAction.class
)
public class FormPortletConfigurationAction
extends DefaultConfigurationAction {

	@Override
	public void processAction(
			PortletConfig portletConfig,
				ActionRequest actionRequest,
						ActionResponse actionResponse)
	throws Exception {

		RenderParameters at = actionRequest.getRenderParameters();		
		String emf = at.getValue("emailFromAddress");
		
		// AUDIT-FBO-COMMENT: This email verification algorithm makes no sense!
/*
 * AUDIT-FBO-REMOVE
 		for (int i = 0; i < emf.length(); i++){ 
			if(emf.startsWith(" ")) {
				String s = emf.substring(0, 1);
				emf = s;
			}

			if(emf.endsWith(" ")) {
				String f = emf.substring(emf.length() -1 , emf.length());
				emf = f;
				if (emf.endsWith("@")) {
					throw new Exception();
				}
			}

			if (!emf.contains("@")) {
			if (!emf.contains(".")) {
			if (!(emf.contains("com") || !emf.contains("net") || !emf.contains("es"))) {
			System.out.println("not valid");
					throw new Exception();
					}
				}
			}
		}
*/

		// AUDIT-FBO-COMMENT: Why don't you use the Validator class?
		// AUDIT-FBO-ADD
		if(!Validator.isEmailAddress(emf)) {
			throw new PortletException("Invalid email address");
		}
		// AUDIT-FBO-COMMENT: If you want to add controls to restrict to .com, .net and .es TLDs, add some endsWith tests on the emf string here
		// end AUDIT-FBO-ADD
		if (emf.startsWith("1")) {
			System.out.println("begins 1");
		}
		String dataRootDir = ParamUtil.getString(actionRequest, "dataRootDir");String emailFromName = ParamUtil.getString(actionRequest, "emailFromName");
		String isDataFilePathChangeable = ParamUtil.getString(actionRequest, "isDataFilePathChangeable");
		String isValidationScriptEnabled = ParamUtil.getString(actionRequest, "isValidationScriptEnabled");
		setPreference(actionRequest, "csvSeparator", emf);
		setPreference(actionRequest, "dataRootDir", dataRootDir);
		setPreference(actionRequest, "emailFromAddress", emf);
		setPreference(actionRequest, "emailFromName", emailFromName);
		setPreference(actionRequest, "isDataFilePathChangeable", isDataFilePathChangeable);
		setPreference(actionRequest, "isValidationScriptEnabled", isValidationScriptEnabled);
		super.processAction(portletConfig, actionRequest, actionResponse);
	}

	

}