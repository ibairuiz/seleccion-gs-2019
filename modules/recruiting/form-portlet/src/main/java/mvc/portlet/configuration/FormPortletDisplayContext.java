package mvc.portlet.configuration;


import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;


@Component
public class FormPortletDisplayContext {

	// AUDIT-FBO-COMMENT: Use a Logger instead of System.out.println
	// AUDIT-FBO-ADD:
	private static final Log _log = LogFactoryUtil.getLog(FormPortletDisplayContext.class);
	// end AUDIT-FBO-ADD

    private ConfigurationProvider c;
    public FormPortletConfiguration getFormPortletConfiguration() {
        return fpc;
    }
    public FormPortletDisplayContext(final HttpServletRequest req) {
        super();

        ThemeDisplay t;
        t = (ThemeDisplay) req.getAttribute(WebKeys.THEME_DISPLAY);
        PortletDisplay portletDisplay = t.getPortletDisplay();
        if (null == portletDisplay.getId()) {
                    Layout l = t.getLayout();
    try {

                fpc = c.getPortletInstanceConfiguration(
                    FormPortletConfiguration.class, l, PortalUtil.getPortletId(req));
                        } catch (Exception ce) {
                
                        }
                        } else {

                try {
            fpc = portletDisplay.getPortletInstanceConfiguration(FormPortletConfiguration.class);
                            } catch (Exception ce) {
                    			// AUDIT-FBO-COMMENT: Use a logger and be more explicit about what you log
                    			// AUDIT-FBO-REMOVE: ce.printStackTrace();
                    			// AUDIT-FBO-ADD: 
                    			_log.error("Error while getting portlet instance configuration", ce);
                    			// end AUDIT-FBO-ADD				                            	
        
        }
        }
    }
    public FormPortletDisplayContext() {
        super();
    }
    public FormPortletDisplayContext(final PortletRequest req) {
        this(PortalUtil.getHttpServletRequest(req));
    }


	private FormPortletConfiguration fpc = null;
    @Reference
    protected void setConfigurationProvider(ConfigurationProvider cp) {
        this.c = cp;
    }





}
