package usr.umf;

import umf.common.info.ManagementInfoSpecification;
import umf.common.info.InfoCfgSetting;

public class DemoVocabulary {



    /*
     * Virtual Infrastructure Management NEM
     */
    // Inputs and Outputs
    public final static String STR_UIS_VIM_ROUTERS_DETAIL = "VIMRoutersDetail";
    public final static String STR_UIS_VIM_LINKS_DETAIL = "VIMLinksDetail";
    public final static String STR_UIS_VIM_REMOVED = "VIMRemoved";
	
    public static final ManagementInfoSpecification VIMRoutersDetailMIS = new ManagementInfoSpecification(InfoCfgSetting.class, Number.class, STR_UIS_VIM_ROUTERS_DETAIL);	
    public static final ManagementInfoSpecification VIMLinksDetailMIS = new ManagementInfoSpecification(InfoCfgSetting.class, Number.class, STR_UIS_VIM_LINKS_DETAIL);
    public static final ManagementInfoSpecification VIMRemovedMIS = new ManagementInfoSpecification(InfoCfgSetting.class, Number.class, STR_UIS_VIM_REMOVED);

}
