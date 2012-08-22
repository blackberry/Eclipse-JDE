/*
* Copyright (c) 2010-2012 Research In Motion Limited. All rights reserved.
*
* This program and the accompanying materials are made available
* under the terms of the Eclipse Public License, Version 1.0,
* which accompanies this distribution and is available at
*
* http://www.eclipse.org/legal/epl-v10.html
*
*/
package net.rim.ejde.internal.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    public static String BlackBerryProjectFormEditor_Res_Chng_Diag_Msg;
    public static String BlackBerryProjectFormEditor_Res_Chng_Diag_Title;

    public static String BlackBerryProjectPropertiesLinkPage_Text;
    public static String BlackBerryVMInstallPage_BBVMInstallPageIcon;
    public static String BlackBerryVMInstallPage_BBVMInstallPageTitle;
    public static String BlackBerryVMInstallType_EE_Prop_Err_Msg1;
    public static String BlackBerryVMInstallType_EE_Prop_Err_Msg2;
    public static String BlackBerryVMInstallType_Location_Err_Msg;
    public static String BrowseSearchDialog_browseLabel;
    public static String BrowseSearchDialog_browseTitle;
    public static String BrowseSearchDialog_dontAskLabel;
    public static String BrowseSearchDialog_searchExceptionMsg;
    public static String BrowseSearchDialog_searchingLabel;
    public static String BrowseSearchDialog_searchLabel;
    public static String BrowseSearchDialog_message;

    public static String SignatureToolPasswordPromptDialog_DialogButtonMsg;
    public static String SignatureToolPasswordPromptDialog_DialogTitleMsg;
    public static String SignatureToolPasswordPromptDialog_ErrorDialogMsg;
    public static String SignatureToolPasswordPromptDialog_ErrorDialogTitleMsg;
    public static String SignatureToolPrefsPage_SilentToolBtnMsg;
    public static String SignatureToolPrefsPage_SilentToolBtnTooltipMsg;
    public static String SignatureToolPrefsPage_AutomaticallySigningBtnMsg;
    public static String SignatureToolPrefsPage_AutomaticallySigningBtnTooltipMsg;
    public static String SignatureTool_Not_Found_Msg;
    public static String JavaLoader_Not_Found_Msg;

    public static String SignCommandHandler_MissingFilesDialogMsg;
    public static String SignCommandHandler_MissingFilesDialogTitleMsg;
    public static String SignCommandHandler_SigToolRunningDialogMsg;
    public static String SignCommandHandler_SigToolRunningDialogTitleMsg;
    public static String SignCommandHandler_MissingFileMsg;

    public static String SigningSearchDialog_ExceptionMessage1;
    public static String SigningSearchDialog_ExceptionMessage2;
    public static String SigningSearchDialog_ExceptionMessage3;
    public static String SigningSearchDialog_DirDialogTitleMsg;
    public static String SigningSearchDialog_ExceptionMessage4;
    public static String SigningSearchDialog_ExceptionMessage5;
    public static String SigningSearchDialog_ExceptionMessage6;
    public static String SigningSearchDialog_ExceptionMessage7;

    public static String ClasspathChangeManager_PROJECT_NATURE_ERROR;
    public static String ClasspathChangeManager_settingJDKComplianceMsg1;
    public static String ClasspathChangeManager_settingJDKComplianceMsg2;
    public static String ClasspathChangeManager_settingJDKComplianceMsg3;
    public static String ClasspathChangeManager_rebuildProjectMsg1;
    public static String ClasspathChangeManager_rebuildProjectMsg2;
    public static String ClasspathChangeManager_RebuildProjectDialogTitle;
    public static String ClasspathChangeManager_DialogTitle;
    public static String CleanProjectsDialog_buildCleanAuto;
    public static String CleanProjectsDialog_buildCleanManual;
    public static String CleanProjectsDialog_PreprocessTag_description;
    public static String CleanProjectsDialog_CodeSigning_description;
    public static String CleanProjectsDialog_description;
    public static String ComponentPackHandler_Undefined_Object_Argument_Err_Msg;
    public static String ErrorHandler_DIALOG_TITLE;
    public static String IConstants_ADD_BUTTON_TITLE;
    public static String IConstants_BROWSE_BUTTON_TITLE;
    public static String IConstants_CANCEL_BUTTON_TITLE;
    public static String IConstants_DELETE_BUTTON_TITLE;
    public static String IConstants_EDIT_BUTTON_TITLE;
    public static String IConstants_ERROR_DIALOG_TITLE;
    public static String IConstants_IConstants_REFRESH_BUTTON_TITLE;
    public static String IConstants_Yes_BUTTON_TITLE;
    public static String IConstants_No_BUTTON_TITLE;
    public static String IConstants_OK_BUTTON_TITLE;
    public static String IConstants_RUN_BUTTON_TITLE;
    public static String IConstants_SEARCH_BUTTON_TITLE;
    public static String ObjectViewPathToDialog_FROM_LABEL_TITLE;
    public static String ObjectViewPathToDialog_INVALID_INPUT_MESSAGE;
    public static String RemoveEntryDialog_Title;
    public static String RemoveEntryDialog_Text;
    public static String RIADialog_invalidFileMsg;
    public static String RIADialog_noDebugFileSelected;
    public static String RimIDEUtil_NULL_VERSION_MESSAGE;
    public static String RimIDEUtil_PLUGIN_NOT_FOUND_MESSAGE;
    public static String RimIDEUtil_SAVE_FILE_DIALOG_TITLE;

    public static String LegacyImportOperation_ERROR_INDICATION_MSG;
    public static String LegacyImportOperation_PROJECT_IMPORT_FAIL_MSG;
    public static String LegacyImportOperation_WRONG_IMPORT_TYPE_MSG;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // General messages
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String DontAskMeAgainMsg;
    public static String MissingVC2008WarningTitle;
    public static String MissingVC2008WarningMsg;
    public static String VC2008DownloadLink;
    public static String NewProject_perspSwitchMessage;
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // NEW PROJECT WIZARD
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String NewElementWizard_op_error_title;
    public static String NewElementWizard_op_error_message;
    public static String BlackBerryProjectWizard_title;
    public static String BlackBerryProjectWizard_op_error_title;
    public static String BlackBerryProjectWizard_op_error_create_message;
    public static String NewBlackBerryProjectWizardPageOne_directory_message;
    public static String NewBlackBerryProjectWizardPageOne_JREGroup_link_description;
    public static String NewBlackBerryProjectWizardPageOne_JREGroup_default_compliance;
    public static String NewBlackBerryProjectWizardPageOne_JREGroup_specific_compliance;
    public static String NewBlackBerryProjectWizardPageOne_JREGroup_specific_EE;
    public static String NewBlackBerryProjectWizardPageOne_JREGroup_title;
    public static String NewBlackBerryProjectWizardPageOne_page_description;
    public static String NewBlackBerryProjectWizardPageOne_page_title;
    public static String NewBlackBerryProjectWizardPageOne_NoJREFound_link;
    public static String NewBlackBerryProjectWizardPageOne_LayoutGroup_link_description;
    public static String NewBlackBerryProjectWizardPageOne_LayoutGroup_option_oneFolder;
    public static String NewBlackBerryProjectWizardPageOne_DetectGroup_differendWorkspaceCC_message;
    public static String NewBlackBerryProjectWizardPageOne_Message_invalidProjectNameForWorkspaceRoot;
    public static String NewBlackBerryProjectWizardPageOne_Message_cannotCreateAtExternalLocation;
    public static String NewBlackBerryProjectWizardPageOne_Message_notExisingProjectOnWorkspaceRoot;
    public static String NewBlackBerryProjectWizardPageOne_Message_noBBJREInstalled;
    public static String NewBlackBerryProjectWizardPageOne_Message_noJRESelected;
    public static String NewBlackBerryProjectWizardPageOne_Message_nonBBDefaultJRESelected;
    public static String NewBlackBerryProjectWizardPageOne_LayoutGroup_option_separateFolders;
    public static String NewBlackBerryProjectWizardPageOne_LayoutGroup_title;
    public static String NewBlackBerryProjectWizardPageOne_WorkingSets_group;
    public static String NewBlackBerryProjectWizardPageOne_LocationGroup_title;
    public static String NewBlackBerryProjectWizardPageOne_LocationGroup_external_desc;
    public static String NewBlackBerryProjectWizardPageOne_LocationGroup_browseButton_desc;
    public static String NewBlackBerryProjectWizardPageOne_LocationGroup_locationLabel_desc;
    public static String NewBlackBerryProjectWizardPageOne_LocationGroup_workspace_desc;
    public static String NewBlackBerryProjectWizardPageOne_NameGroup_label_text;
    public static String NewBlackBerryProjectWizardPageOne_DetectGroup_jre_message;
    public static String NewBlackBerryProjectWizardPageOne_DetectGroup_message;
    public static String NewBlackBerryProjectWizardPageOne_Message_enterLocation;
    public static String NewBlackBerryProjectWizardPageOne_Message_enterProjectName;
    public static String NewBlackBerryProjectWizardPageOne_Message_invalidDirectory;
    public static String NewBlackBerryProjectWizardPageOne_Message_notOnWorkspaceRoot;
    public static String NewBlackBerryProjectWizardPageOne_Message_projectAlreadyExists;
    public static String NewBlackBerryProjectWizardPageOne_UnknownDefaultJRE_name;
    public static String NewBlackBerryProjectWizardPageOne_ExistingSrcLocOverlapsWSMsg;
    public static String NewBlackBerryProjectWizardPageTwo_error_remove_message;
    public static String NewBlackBerryProjectWizardPageTwo_error_remove_title;
    public static String NewBlackBerryProjectWizardPageTwo_problem_backup;
    public static String NewBlackBerryProjectWizardPageTwo_DeleteCorruptProjectFile_message;
    public static String NewBlackBerryProjectWizardPageTwo_monitor_init_build_path;
    public static String NewBlackBerryProjectWizardPageTwo_problem_restore_classpath;
    public static String NewBlackBerryProjectWizardPageTwo_problem_restore_project;
    public static String NewBlackBerryProjectWizardPageTwo_operation_create;
    public static String NewBlackBerryProjectWizardPageTwo_operation_remove;
    public static String NewBlackBerryProjectWizardPageTwo_operation_initialize;
    public static String NewBlackBerryProjectWizardPageTwo_error_title;
    public static String NewBlackBerryProjectWizardPageTwo_error_message;
    public static String NewBlackBerryProjectWizardPageTwo_error_dialog_title;
    public static String NewBlackBerryProjectWizardPageTwo_error_dialog_message1;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // NEW RESOURCE WIZARD
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String newResourceFileWindowsTitle;
    public static String NewResourceFileWizardDescription;
    public static String NewResourceFileWizardNewProjectTitle;
    public static String INVALID_FILE_NAME;
    public static String INVALID_PARENT_FOLDER_WIZARD_PAGE;
    public static String INVALID_PACKAGE_SELECTED;
    public static String RRH_NO_PACKAGE_ERROR;
    public static String EXISTING_COPIED_SIBLING_FOUND_WARNING;
    public static String EXISTING_LINKED_SIBLING_FOUND_WARNING;
    public static String IMAGE_DESCRIPTOR_FILE_PATH;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // RESOURCE EDITOR
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String MESSAGE_BOX_OPEN_TEXT_EDITOR_PROMPT;
    public static String MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_WORKSPACE;
    public static String MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_DIRECTORY;
    public static String MESSAGE_BOX_OPEN_TEXT_EDITOR_PROMPT_TITLE;
    public static String MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_WORKSPACE_TITLE;
    public static String MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_DIRECTORY_TITLE;
    public static String NO_LOCALE_ERROR_TITLE;
    public static String NO_LOCALE_ERROR_TEXT;
    public static String MISSING_LOCALE_ERROR_TITLE;
    public static String MISSING_LOCALE_ERROR_TEXT;
    public static String AddButton_text;
    public static String InputDialog_message;
    public static String MessageDialog_title;
    public static String MessageDialog_ResourceNameEmpty;
    public static String ResourceKeyValidator_ResourceKey_Empty;
    public static String ResourceKeyValidator_ResourceKey_Whitespace;
    public static String ResourceKeyValidator_ResourceKey_KeyExists;
    public static String ResourceKeyValidator_ResourceKey_ReservedWord;
    public static String ResourceKeyValidator_ResourceKey_InvalidCharacter;
    public static String ResourceKeyValidator_ResourceKey_EqualsCurrentKey;
    public static String FILE_READ_ONLY_TEXT;
    public static String INVALID_PACKAGE_TITLE;
    public static String INVALID_PACKAGE_TEXT;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // BlackBerry Descriptor Editor
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String AlternateEntryPointDetails_Description;
    public static String AlternateEntryPointDetails_Title;
    public static String AlternateEntryPointSection_Add_Dialog_Label;
    public static String AlternateEntryPointSection_Add_Dialog_Title;
    public static String AlternateEntryPointSection_Description;
    public static String AlternateEntryPointSection_Title;
    public static String AlternateEntryPointSection_ToolTip;
    public static String AlternateEntryPointDetailsSection_Title_ToolTip;
    public static String ALXFilesSection_Add_Dialog_Filter_Name;
    public static String ALXFilesSection_Description;
    public static String ALXFilesSection_Title;
    public static String BlackBerryProjectAlternateEntryPointPage_ID;
    public static String BlackBerryProjectAlternateEntryPointPage_Page_Title;
    public static String BlackBerryProjectAlternateEntryPointPage_Page_Title_Disabled;
    public static String BlackBerryProjectAlternateEntryPointPage_Title;
    public static String BlackBerryProjectApplicationPage_ID;
    public static String BlackBerryProjectApplicationPage_Page_Title;
    public static String BlackBerryProjectApplicationPage_Title;
    public static String BlackBerryProjectBuildPage_ID;
    public static String BlackBerryProjectBuildPage_Page_Title;
    public static String BlackBerryProjectBuildPage_Title;
    public static String BlackBerryProjectPropertiesPage_Add_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Add_From_External_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Add_From_Project_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Add_File_Error_Status_Invalid_File;
    public static String BlackBerryProjectPropertiesPage_Add_Icon_Error_Status_Max_Two;
    public static String BlackBerryProjectPropertiesPage_Add_Icon_Message;
    public static String BlackBerryProjectPropertiesPage_Add_Icon_Title;
    public static String BlackBerryProjectPropertiesPage_Dup_File_Err_Dialog_Title;
    public static String BlackBerryProjectPropertiesPage_Dup_Icon_Err_Dialog_Msg;
    public static String BlackBerryProjectPropertiesPage_Dup_File_Err_Status_Msg;
    public static String BlackBerryProjectPropertiesPage_Dup_File_Link_Err_Dialog_Msg;
    public static String BlackBerryProjectPropertiesPage_Browse_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Browse_Button_Label_Ellipsis;
    public static String BlackBerryProjectPropertiesPage_MoveUp_Button_Label;
    public static String BlackBerryProjectPropertiesPage_MoveDown_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Deselect_All_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Dialog_Filter_All_Files;
    public static String BlackBerryProjectPropertiesPage_Dialog_Filter_All_Files_2;
    public static String BlackBerryProjectPropertiesPage_Dialog_Filter_Image_Files;
    public static String BlackBerryProjectPropertiesPage_Edit_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Remove_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Select_All_Button_Label;
    public static String BlackBerryProjectPropertiesPage_Table_File_Column_Label;
    public static String BlackBerryProjectPropertiesPage_Table_RolloverIcon_Column_Label;
    public static String BlackBerryProjectPropertiesPage_Table_Icon_Column_Label;
    public static String BlackBerryProjectPropertiesPage_Table_Title;
    public static String CompilerToProjectEditorManager_libWarnMsg;

    public static String CompileSection_Alias_List_Label;
    public static String CompileSection_Alias_List_ToolTip;
    public static String CompileSection_Compress_Resources_Label;
    public static String CompileSection_Compress_Resources_ToolTip;
    public static String CompileSection_Convert_Images_Label;
    public static String CompileSection_Create_Warning_Label;
    public static String CompileSection_Description;
    public static String CompileSection_Output_Messages_Label;
    public static String CompileSection_Title;
    public static String GeneralSection_Application_Argument_Label;
    public static String GeneralSection_Application_Type_Label;
    public static String GeneralSection_Application_Type_ToolTip;
    public static String GeneralSection_Auto_Run_Label;
    public static String GeneralSection_Description;
    public static String GeneralSection_Description_Label;
    public static String GeneralSection_Home_Screen_Position_Label;
    public static String GeneralSection_Main_Midlet_Label;
    public static String GeneralSection_Startup_Tier_Label;
    public static String GeneralSection_System_Module_Label;
    public static String GeneralSection_System_Module_ToolTip;
    public static String GeneralSection_Title;
    public static String GeneralSection_Title_Label;
    public static String GeneralSection_Title_ToolTip;
    public static String GeneralSection_Vendor_Label;
    public static String GeneralSection_Vendor_ToolTip;
    public static String GeneralSection_Version_Label;
    public static String GeneralSection_Version_ToolTip;
    public static String IconsSection_Description;
    public static String IconsSection_Title;
    public static String IconSection_Application_Icons_ToolTip;
    public static String JadSection_Description;
    public static String JadSection_Dialog_Label;
    public static String JadSection_Dialog_Single_Selection_Error;
    public static String JadSection_Dialog_Title;
    public static String JadSection_Dialog_Valid_Error;
    public static String JadSection_Jad_Location_Label;
    public static String JadSection_Title;
    public static String JadSection_Use_Custom_Jad_Label;
    public static String PackagingSection_Clean_Step_Label;
    public static String PackagingSection_Description;
    public static String PackagingSection_File_Name_Label;
    public static String PackagingSection_Folder_Label;
    public static String PackagingSection_Generate_ALX_Label;
    public static String PackagingSection_Generate_ALX_ToolTip;
    public static String PackagingSection_Post_Build_Step_Label;
    public static String PackagingSection_Pre_Build_Step_Label;
    public static String PackagingSection_Title;
    public static String PreprocessorTagSection_Description;
    public static String ResourcesSection_Description;
    public static String ResourcesSection_Resource_Available_Label;
    public static String ResourcesSection_Resource_Available_ToolTip;
    public static String ResourcesSection_Resource_Bundle_Label;
    public static String ResourcesSection_Resource_Description_Label;
    public static String ResourcesSection_Resource_Description_ToolTip;
    public static String ResourcesSection_Resource_Title_Label;
    public static String ResourcesSection_Title;
    public static String ResourcesSection_ToolTip;
    public static String ResourcesSection_invalidResource;
    public static String ResourcesSection_invalidResourceKey;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // BUILDERS
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String ResourceBuilder_WRONG_PACKAGE_MSG;
    public static String ResourceBuilder_NO_PACKAGE_INFO_MSG;
    public static String RIMResourcesBuilder_COMPILE_FILE_ERROR_MSG;
    public static String RIMResourcesBuilder_ResourceInterfaceFolderMissingMessage;
    public static String RIMResourcesBuilder_RESOURCE_OUTPUT_ROOT_NOT_EXIST_MSG;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // IMPORT
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String BlackBerryImporterWizard_IMPORT_ERROR_MSG;
    public static String GenericSelectionPage_COPY_MODEL_LABEL;
    public static String GenericSelectionPage_DEPENDENCY_ERROR_MSG;
    public static String GenericSelectionPage_FILE_NOT_EXIST_MSG;
    public static String GenericSelectionPage_PROJECT_TABLE_TITLE;
    public static String GenericSelectionPage_WORKSPACE_LABEL;
    public static String GenericSelectionPage_WORKSPACE_NOT_LOADED_MSG;
    public static String GenericSelectionPage_SOME_PROJECTS_EXIST_MSG;
    public static String GenericSelectionPage_NO_WORKSPACE_SELECTED_MSG;
    public static String GenericSelectionPage_NO_WORKSPACE_LOADED_MSG;
    public static String GenericSelectionPage_IMPORT_PAGE_DESCRIPTION;
    public static String GenericSelectionPage_IMPORT_PAGE_TITLE;
    public static String GenericSelectionPage_NO_PROJECT_SELECTED_ERROR_MSG;
    public static String GenericSelectionPage_SAMPLES_IMPORT_PAGE_DESCRIPTION;
    public static String GenericSelectionPage_SAMPLES_IMPORT_PAGE_TITLE;
    public static String BLACKBERRY_WORKSPACE_FILTER_NAME;
    public static String BLACKBERRY_PROJECT_FILTER_NAME;
    public static String BLACKBERRY_WORKSPACE_PROJECT_FILTER_NAME;
    public static String ImportSamplesWizard_SAMPLE_IMPORT_TITLE;
    public static String ImportLegacyProjects_WIZARD_TITLE_LABEL;
    public static String ImportExistingProject_EXISTING_PROJECT_WIZARD_LABEL;
    public static String ImportSampleProjects_SAMPLE_PROJECT_IMPORT;
    public static String ImportLegacyProjects_WIZARD_PAGE_TITLE;
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // Packaging
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String PackagingManager_PACKAGING_PROJECT_MSG;
    public static String PackagingManager_PACKAGING_SUCCEED_MSG;
    public static String PackagingManager_PACKAGING_FAILED_MSG;
    public static String PackagingManager_PACKAGING_CANNOT_CREATE_FOLDER_MSG;
    public static String PackagingManager_PACKAGING_NO_BB_JRE_MSG;
    public static String PackagingManager_Variable_Not_Defined_MSG;
    public static String PackagingManager_MIDLET_JAR_ERROR_MSG1;
    public static String PackagingManager_MIDLET_JAR_ERROR_MSG2;
    public static String PackagingManager_MIDLET_JAR_ERROR_MSG3;
    public static String PackagingManager_MIDLET_JAR_ERROR_MSG4;
    public static String PackagingManager_Entry_Not_Found_MSG;
    public static String PackagingJob_Name;
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // RAPCFile
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String RAPCFIlE_FILE_NOT_FOUND_MSG1;
    public static String RAPCFIlE_FILE_NOT_FOUND_MSG2;
    public static String RAPCFIlE_NO_KEY_MSG;
    public static String RAPCFIlE_RESOURCE_KEY_NOT_FOUND_MSG;
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // PREFERENCE PAGE
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String BasicPrefsPage_AddButtonLabel;
    public static String BasicPrefsPage_RemoveButtonLabel;
    public static String BasicPrefsPage_EditButtonLabel;
    public static String BasicPrefsPage_SelectAllButtonLabel;
    public static String BasicPrefsPage_DeselectAllButtonLabel;

    public static String WorkspacePrefsPage_GeneralTab;
    public static String WorkspacePrefsPage_ProjectVersion;
    public static String WorkspacePrefsPage_ProjectVendor;
    public static String WorkspacePrefsPage_OpenAppDescriptorOnNew;
    public static String WorkspacePrefsPage_OpenStartupOnNew;
    public static String WorkspacePrefsPage_CheckForNewVersion;
    public static String WorkspacePrefsPage_AppendLogToFile;

    public static String BuildPrefsPage_PreprocessTab;
    public static String BuildPrefsPage_PreprocessDefine;
    public static String BuildPrefsPage_PreprocessAddDialogTitle;
    public static String BuildPrefsPage_PreprocessEditDialogTitle;
    public static String BuildPrefsPage_PreprocessDialogLabel;
    public static String BuildPrefsPage_PreprocessEditLabel;
    public static String BuildPrefsPage_PreprocessActiveColulmnHeader;
    public static String BuildPrefsPage_PreprocessTagsColulmnHeader;
    public static String BuildPrefsPage_PreprocessScopeColulmnHeader;
    public static String BuildPrefsPage_PreprocessValidationMsg1;
    public static String BuildPrefsPage_PreprocessValidationMsg2;
    public static String BuildPrefsPage_PreprocessValidationMsg3;
    public static String BuildPrefsPage_PreprocessValidationMsg4;

    public static String CodeSigningPrefsPage_SigningStatusLabel;
    public static String CodeSigningPrefsPage_ClickHereLabel;
    public static String CodeSigningPrefsPage_AddNewKeyLabel;
    public static String CodeSigningPrefsPage_AddNewKeyToolTip;
    public static String CodeSigningPrefsPage_AddOldKeyLabel;
    public static String CodeSigningPrefsPage_AddOldKeyToolTip;
    public static String CodeSigningPrefsPage_RemoveCurrentKeyLabel;
    public static String CodeSigningPrefsPage_RemoveCurrentKeyToolTip;
    public static String CodeSigningPrefsPage_MessageDialogTitle1;
    public static String CodeSigningPrefsPage_MessageDialogTitle2;
    public static String CodeSigningPrefsPage_MessageDialogTitle3;
    public static String CodeSigningPrefsPage_MessageDialogMsg1;
    public static String CodeSigningPrefsPage_MessageDialogMsg2;
    public static String CodeSigningPrefsPage_MessageDialogMsg3;
    public static String CodeSigningPrefsPage_MessageDialogMsg4;
    public static String CodeSigningPrefsPage_MessageDialogMsg5;
    public static String CodeSigningPrefsPage_MessageDialogMsg6;
    public static String CodeSigningPrefsPage_MessageDialogMsg7;
    public static String CodeSigningPrefsPage_MessageDialogMsg8;
    public static String CodeSigningPrefsPage_MessageDialogMsg9;

    public static String DebugPrefsPage_WarnForDebugMsg;
    public static String DebugPrefsPage_WarnDebugBorderLabel;

    public static String SDKPrefsPage_WarnForMissingDependenciesMsg;

    // Utilities
    public static String PackageUtils_PACKAGE_ID_ERROR_MSG;
    public static String PackageUtils_RRH_FILE_NOT_EXIST;
    public static String PackageUtils_RELATED_RRH_FILE_ERROR_MESSAGE;
    public static String PackageUtils_UNDEFINED_FILE_ERROR_MSG;
    public static String PackageUtils_CLOSED_FILE_ERROR_MSG;
    public static String PackageUtils_EMPTY_FILE_ERROR_MSG;
    public static String PackageUtils_UNSUPPORTED_FILE_ERROR_MSG;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // VALIDATION
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String Diagnostic_OK;
    public static String Diagnostic_CANCEL;
    public static String DiagnosticFactory_File_Does_Not_Exist_Error;
    public static String DiagnosticFactory_Home_Screen_Error;
    public static String DiagnosticFactory_Icon_Does_Not_Exist_Error;
    public static String DiagnosticFactory_Invalid_Output_Path_Char_Error;
    public static String DiagnosticFactory_Properties_Cannot_Be_Blank;
    public static String DiagnosticFactory_Value_Required_Error;
    public static String DiagnosticFactory_OutputFN_Must_Differ;
    public static String DiagnosticFactory_System_Module_Problematic;
    public static String BBProjectValidator_MISSING_PROJECT_DESCRIPTION_ERROR;
    public static String BBPropertiesValidator_PATH_INVALID_ERROR;
    public static String BBPropertiesValidator_PATH_NOT_RELATIVE_ERROR;
    public static String BBPropertiesValidator_PATH_NOT_INWARDS_ERROR;

    // Project dependency
    public static String ClasspathChangeManager_WrongProjectDependencyMessage;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // Problem Factory
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String ProblemFactory_VM_missing_err_msg;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // LAUNCH CONFIGURATION
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String SimulatorConfigurationTab_tabName;
    public static String AppSelectionUI_counter;
    public static String AppSelectionUI_selectAll;
    public static String AppSelectionUI_deselectAll;
    public static String AppSelectionUI_selectWorkingSet;

    public static String ProjectsTab_noProjectSelected;
    public static String SimulatorTab_DIR_DIALOG_TITLE;
    public static String AbstractLaunchConfigurationDelegate_debuggerActiveMsg;
    public static String AbstractLaunchConfigurationDelegate_invalidDebugDestMsg;
    public static String DeviceLaunchConfigurationDelegate_noDeviceMsg;
    public static String DeviceLaunchConfigurationDelegate_runErrorMsg;
    public static String FledgeLaunchConfigurationDelegate_noProfileMsg;
    public static String FledgeLaunchConfigurationDelegate_noWorkspaceMsg;
    public static String FledgeLaunchConfigurationDelegate_launchMsg;
    public static String FledgeLaunchConfigurationDelegate_noMDSCSMsg;
    public static String FledgeLaunchConfigurationDelegate_noLaunchMDSCSMsg;
    public static String FledgeLaunchConfigurationDelegate_noJavaMDSCSMsg;
    public static String FledgeLaunchConfigurationDelegate_clickNoMDSCSMsg;
    public static String FledgeLaunchConfigurationDelegate_differentJRE;
    public static String FledgeLaunchConfigurationDelegate_differentDevice;
    public static String DeviceConfigurationTab_attachLabel;
    public static String DeviceConfigurationTab_attachToolTip;
    public static String DeviceConfigurationTab_groupLabel;
    public static String DeviceConfigurationTab_invalidDeviceMsg;
    public static String DeviceConfigurationTab_noDeviceMsg;
    public static String DeviceConfigurationTab_tabName;
    public static String RunningSimulatorConfigurationTab_disconnectedSimMsg;
    public static String RunningSimulatorConfigurationTab_groupLabel;
    public static String RunningSimulatorConfigurationTab_name;
    public static String RunningSimulatorConfigurationTab_noSimMsg;
    public static String RunningSimulatorConfigurationTab_simChoiceLabel;
    public static String RunningSimulatorConfigurationTab_simChoiceToolTip;
    public static String SimulatorOutputView_DEBUG_MODE_ONLY;
    public static String SimulatorConfigurationTab_jreSelectorLabel;
    public static String SimulatorConfigurationTab_generalTabLabel;
    public static String SimulatorConfigurationTab_debuggingTabLabel;
    public static String SimulatorConfigurationTab_memoryTabLabel;
    public static String SimulatorConfigurationTab_networkTabLabel;
    public static String SimulatorConfigurationTab_portsTabLabel;
    public static String SimulatorConfigurationTab_viewTabLabel;
    public static String SimulatorConfigurationTab_advancedTabLabel;
    public static String SimulatorConfigurationTab_General_launchMDSCS;
    public static String SimulatorConfigurationTab_General_launchApp;
    public static String SimulatorConfigurationTab_General_device;
    public static String SimulatorConfigurationTab_General_deviceTooltip;
    public static String SimulatorConfigurationTab_General_autoUseDefaultValue;
    public static String SimulatorConfigurationTab_General_timeToWait;
    public static String SimulatorConfigurationTab_General_pin;
    public static String SimulatorConfigurationTab_General_esn;
    public static String SimulatorConfigurationTab_General_meid;
    public static String SimulatorConfigurationTab_General_meidTooltip;
    public static String SimulatorConfigurationTab_General_enableDeviceSecurity;
    public static String SimulatorConfigurationTab_General_enableDeviceSecurityTooltip;
    public static String SimulatorConfigurationTab_General_systemLocale;
    public static String SimulatorConfigurationTab_General_systemLocaleTooltip;
    public static String SimulatorConfigurationTab_General_keyboardLocale;
    public static String SimulatorConfigurationTab_General_keyboardLocaleTooltip;
    public static String SimulatorConfigurationTab_General_eraseSimulatorFile;
    public static String SimulatorConfigurationTab_General_eraseRemovable;
    public static String SimulatorConfigurationTab_General_eraseNonVolatile;
    public static String SimulatorConfigurationTab_General_eraseFileSystem;
    public static String SimulatorConfigurationTab_Debugging_interrupt;
    public static String SimulatorConfigurationTab_Debugging_nonStopExecution;
    public static String SimulatorConfigurationTab_Memory_HeapSize;
    public static String SimulatorConfigurationTab_Memory_brandingData;
    public static String SimulatorConfigurationTab_Memory_resetFileSystem;
    public static String SimulatorConfigurationTab_Memory_resetFileSystemTooltip;
    public static String SimulatorConfigurationTab_Memory_resetNVRam;
    public static String SimulatorConfigurationTab_Memory_resetNVRamTooltip;
    public static String SimulatorConfigurationTab_Memory_fileSystemSize;
    public static String SimulatorConfigurationTab_Memory_fileSystemSizeTooltip;
    public static String SimulatorConfigurationTab_Memory_notSaveFlash;
    public static String SimulatorConfigurationTab_Memory_notCompactFS;
    public static String SimulatorConfigurationTab_Memory_simulate_SD_Inserted;
    public static String SimulatorConfigurationTab_Memory_destroyExistingSD;
    public static String SimulatorConfigurationTab_Memory_destroyExistingSDTooltip;
    public static String SimulatorConfigurationTab_Memory_sdSize;
    public static String SimulatorConfigurationTab_Memory_sdSizeTooltip;
    public static String SimulatorConfigurationTab_Memory_sdImage;
    public static String SimulatorConfigurationTab_Memory_usePCForSD;
    public static String SimulatorConfigurationTab_Memory_pcFileSystem;
    public static String SimulatorConfigurationTab_Memory_pcFileSystemTooltip;
    public static String SimulatorConfigurationTab_Network_disableRegistration;
    public static String SimulatorConfigurationTab_Network_networks;
    public static String SimulatorConfigurationTab_Network_networksTooltip;
    public static String SimulatorConfigurationTab_Network_startWithRadioOff;
    public static String SimulatorConfigurationTab_Network_phoneNumbers;
    public static String SimulatorConfigurationTab_Network_autoAnswerOutgoingCall;
    public static String SimulatorConfigurationTab_Network_imei;
    public static String SimulatorConfigurationTab_Network_iccid;
    public static String SimulatorConfigurationTab_Network_imsi;
    public static String SimulatorConfigurationTab_Network_simulateSIMNotPresent;
    public static String SimulatorConfigurationTab_Network_ipAddress;
    public static String SimulatorConfigurationTab_Network_ipAddressTooltip;
    public static String SimulatorConfigurationTab_Network_ignoreUDPConflict;
    public static String SimulatorConfigurationTab_Network_ignoreUDPConflictTooltip;
    public static String SimulatorConfigurationTab_Network_smsSource;
    public static String SimulatorConfigurationTab_Network_smsSourceTooltip;
    public static String SimulatorConfigurationTab_Network_smsDestination;
    public static String SimulatorConfigurationTab_Network_smsDestinationTooltip;
    public static String SimulatorConfigurationTab_Network_pde;
    public static String SimulatorConfigurationTab_Network_pdeTooltip;
    public static String SimulatorConfigurationTab_Port_usbConnected;
    public static String SimulatorConfigurationTab_Port_usbConnectedTooltip;
    public static String SimulatorConfigurationTab_Port_bluetoothPort;
    public static String SimulatorConfigurationTab_Port_bluetoothPortTooltip;
    public static String SimulatorConfigurationTab_View_disableBacklightOff;
    public static String SimulatorConfigurationTab_View_disableBacklightOffTooltip;
    public static String SimulatorConfigurationTab_View_hideNetworkInfo;
    public static String SimulatorConfigurationTab_View_hideNetworkInfoTooltip;
    public static String SimulatorConfigurationTab_View_lcdOnly;
    public static String SimulatorConfigurationTab_View_lcdZoom;
    public static String SimulatorConfigurationTab_View_lcdZoomTooltip;
    public static String SimulatorConfigurationTab_View_lcdBacklightOn;
    public static String SimulatorConfigurationTab_View_keyMapping;
    public static String SimulatorConfigurationTab_View_keyMappingTooltip;
    public static String SimulatorConfigurationTab_Advanced_simulateBattery;
    public static String SimulatorConfigurationTab_Advanced_numPad;
    public static String SimulatorConfigurationTab_Advanced_numPadTooltip;
    public static String SimulatorConfigurationTab_Advanced_default_commandLine;
    public static String SimulatorConfigurationTab_Advanced_default_workingdirectory;
    public static String SimulatorConfigurationTab_Advanced_default_MDSdirectory;
    public static String SimulatorConfigurationTab_Advanced_customized_options;
    public static String SimulatorConfigurationTab_Advanced_customized_commandLine;
    public static String SimulatorConfigurationTab_Advanced_customized_workingdirectory;
    public static String SimulatorConfigurationTab_Advanced_customized_MDSdirectory;
    public static String SimulatorConfigurationTab_openFile;
    public static String SimulatorConfigurationTab_dmpFile;
    public static String SimulatorConfigurationTab_noDeviceSelected;
    public static String SimulatorConfigurationTab_invalidValue;
    public static String ProjectsTab_Projects;
    public static String ProjectsTab_dependencyErrorMsg;
    public static String JRETab_title;
    public static String JRETab_groupTitle;
    public static String JRETab_projectJRE;
    public static String JRETab_alternateJRE;
    public static String JRETab_installedJREs;
    public static String Launch_Close_Simulator_Dialog_Title;
    public static String Launch_Close_Simulator_Dialog_Message;
    public static String Launch_Error_Title;
    public static String Launch_Error_SimulatorNotAvailable;
    public static String Launch_Error_DeviceNotFound;
    public static String Launch_Error_DefaultDeviceNotFound;
    public static String Launch_Error_ProjectNotFound;
    public static String Launch_Error_JRENotFound;
    public static String Launch_Error_HotswapNotSupport;
    public static String Luanch_Error_NoProjectToBeDeployed;
    public static String Launch_Error_DeviceNotFoundInCommnandLine;
    public static String RunningFledgeLaunchConfiguration_projectNotDeploy;
    public static String DeviceLCProjectsTab_deployProjects;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // PROFILER
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String MethodProfileTab_METHOD_TAB_TITLE;
    public static String OptionsDialog_1;
    public static String OptionsDialog_2;
    public static String OptionsDialog_METHOD_ATTRIBUTION_LABEL_TITLE;
    public static String OptionsDialog_OPTIONS_DIALOG_TITLE;
    public static String OptionsDialog_WHATTOPROFILE_LABEL_TITLE;
    public static String ProfilerView_FILTER_LABEL_TITLE;
    public static String ProfilerView_GET_PROFILE_OPTION_ERR_MSG;
    public static String ProfileTab_0;
    public static String ProfileTab_CALLER_TITLE;
    public static String ProfileTab_COUNT_COLUMN_TITLE;
    public static String ProfileTab_DETAILS_COLUMN_TITLE;
    public static String ProfileTab_PERCENT_COLUMN_TITLE;
    public static String SourceProfileTab_NO_SOURCE_MESSAGE;
    public static String SourceProfileTab_PROFILE_DATA_IS_NULL_MSG;
    public static String SourceProfileTab_PROFILE_VIEW_TITLE;
    public static String SourceProfileTab_SOURCE_NOT_FOUND_MESSAGE;
    public static String SourceProfileTab_SOURCE_TAB_TITLE;
    public static String SummaryProfileTab_CODE_EXECUTION_COLUMN_TITLE;
    public static String SummaryProfileTab_FULL_COLUMN_TITLE;
    public static String SummaryProfileTab_GC_COLUMN_TITLE;
    public static String SummaryProfileTab_IDLE_COLUMN_TITLE;
    public static String SummaryProfileTab_RAM_COLUMN_TITLE;
    public static String SummaryProfileTab_SUMMARY_TAB_TITLE;
    public static String AbstractTreeOwnerDrawLabelProvider_UNKNOWN_IMAGE_MESSAGE;
    public static String BasicView_BACKWARD_ACTION_HINT;
    public static String BasicView_BACKWARD_ACTION_TITLE;
    public static String BasicView_CLEAR_ACTION_HINT;
    public static String BasicView_CLEAR_ACTION_TITLE;
    public static String BasicView_COMPARE_ACTION_HINT;
    public static String BasicView_COMPARE_ACTION_TITLE;
    public static String BasicView_FILTER_ACTION_HINT;
    public static String BasicView_FILTER_ACTION_TITLE;
    public static String BasicView_FORWARD_ACTION_HINT;
    public static String BasicView_FORWARD_ACTION_TITLE;
    public static String BasicView_GC_ACTION_HINT;
    public static String BasicView_GC_ACTION_TITLE;
    public static String BasicView_GO_TO_END_ACTION_HINT;
    public static String BasicView_GO_TO_END_ACTION_TITLE;
    public static String BasicView_OPTIONS_ACTION_HINT;
    public static String BasicView_OPTIONS_ACTION_TITLE;
    public static String BasicView_REFRESH_ACTION_HINT;
    public static String BasicView_REFRESH_ACTION_TITLE;
    public static String BasicView_RETURN_TO_START_ACTION_HINT;
    public static String BasicView_RETURN_TO_START_ACTION_TITLE;
    public static String BasicView_SAVE_ACTION_HINT;
    public static String BasicView_SAVE_ACTION_TITLE;
    public static String BasicView_SNAPSHOT_ACTION_HINT;
    public static String BasicView_SNAPSHOT_ACTION_TITLE;
    public static String BasicView_SAVE_XML_ACTION_TITLE;
    public static String BasicView_SAVE_XML_ACTION_HINT;
    public static String BasicView_SAVE_RAW_XML_ACTION_TITLE;
    public static String BasicView_SAVE_RAW_XML_ACTION_HINT;
    public static String BasicView_OPEN_PROFILEVIS_ACTION_TITLE;
    public static String BasicView_OPEN_PROFILEVIS_ACTION_HINT;
    public static String ProfilerView_Refresh;
    public static String ProcessOptionsDialog_TITLE;
    public static String BBProfileVis_Not_Installed_ErrMsg;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // MEMORY STATISTICS VIEW
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String MemoryStatsView_MESSAGE_DIALOG_TITLE;
    public static String MemoryStatsView_SNAPSHOT_NOT_TAKEN_MESSAGE;
    public static String MemoryStatsView_File_Creation_Error_Msg;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // OBJECTS VIEW
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String ObjectsView_LINE_ATTRIBUTE_DIALOG_TITLE;
    public static String ObjectsView_NO_OBJECTS_INFORMATION_MSG;
    public static String ObjectsView_NUMBER_OF_ADDED_OBJECTS_MESSAGE;
    public static String ObjectsView_NUMBER_OF_DELETED_OBJECTS_MESSAGE;
    public static String ObjectsView_NUMBER_OF_OBJECTS_MESSAGE;
    public static String ObjectsView_REFRESH_ERROR_MSG;
    public static String ObjectsView_REFRESH_OBJECTS_VIEW_MSG;
    public static String ObjectsView_RELOADING_FINISH_MSG;
    public static String ObjectsView_RELOADING_MSG;
    public static String ObjectsView_SAVE_SNAPTHO_MSG;
    public static String ObjectsView_SIZE_OF_USED_MEMORY_MESSAGE;
    public static String ObjectsView_VIEW_TITLE;
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // OBJECTS VIEW
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String ProcessView_NO_BB_DEBUG_SESSION_MSG;
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // RIA
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String RIA_NO_RIA_INSTANCE_ERROR_MSG;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // CLEAN SIMULATOR DIALOG
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String CLEAN_SIMULATOR_DIALOG_TITLE;
    public static String CLEAN_SIMULATOR_DIALOG_MESSAGE;
    public static String CLEAN_SIMULATOR_DIALOG_SELECT_ALL_BUTTON;
    public static String CLEAN_SIMULATOR_DIALOG_DESELECT_ALL_BUTTON;
    public static String CLEAN_SIMULATOR_DIALOG_CLEAN_BUTTON;
    public static String CLEAN_SIMULATOR_DIALOG_ERROR_NO_SELECTION;
    public static String CLEAN_SIMULATOR_DIALOG_WARNING_OLDER_SDK;
    public static String CLEAN_SIMULATOR_JOB_TITLE;
    public static String CLEAN_SIMULATOR_ERASE_SIMULATOR_FILES_LABEL;
    public static String CLEAN_SIMULATOR_ERASE_FILE_SYSTEM_LABEL;
    public static String CLEAN_SIMULATOR_ERASE_NON_VOLATILE_MEMORY_LABEL;
    public static String CLEAN_SIMULATOR_ERASE_REMOVABLE_MEMORY_LABEL;
    public static String CLEAN_SIMULATOR_CLEAN_SIMULATOR_DIRECTORY_LABEL;
    public static String CLEAN_SIMULATOR_ERASE_FILE_SYSTEM_DEBUG_MSG;
    public static String CLEAN_SIMULATOR_ERASE_NON_VOLATILE_MEMORY_DEBUG_MSG;
    public static String CLEAN_SIMULATOR_ERASE_REMOVABLE_DIRECTORY_DEBUG_MSG;
    public static String CLEAN_SIMULATOR_CLEAN_SIMULATOR_DIRECTORY_DEBUG_MSG;
    public static String CLEAN_SIMULATOR_CANNOT_DEL_FILE_MSG;
    public static String CLEAN_SIMULATOR_DIR_CORRUPT_MSG;
    public static String CLEAN_SIMULATOR_MANIFEST_PERM_MSG;
    public static String CLEAN_SIMULATOR_MANIFEST_NOT_REPAIR_MSG;
    public static String CLEAN_SIMULATOR_MANIFEST_MISSING_MSG;
    public static String CLEAN_SIMULATOR_CHECK_PERM_MSG;
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // Deployment
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String DeploymentHelper_FILE_NOT_EXIST_MSG;
    public static String DeploymentHelper_FILE_NOT_WRITABLE_MSG;
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // Preprocessor
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String PREDEFINED_PREPROCESS_DEFINE_INFO;
    public static String CUSTOMIZED_PREPROCESS_DEFINE_INFO;
    public static String PreprocessHookCanNotBeConfiguredErr;
    public static String PreprocessHookEclipseIniNotFoundErr;
    public static String PreprocessHookInstallDialogTitle;
    public static String PreprocessHookInstallDialogButtonLabel;
    public static String PreprocessHookInstallDialog_Text;
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Key Editor
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String PrivateKeyEditor_CLEAR_PROTECT_TITLE;
    public static String PrivateKeyEditor_CLEAR_PROTECT_TITLE_TOOLTIP;
    public static String PrivateKeyEditor_CLEARALL_PROTECT_TITLE;
    public static String PrivateKeyEditor_CLEARALL_PROTECT_TITLE_TOOLTIP;
    public static String PrivateKeyEditor_CLEARALL_PROTECT_WARNING_MESSAGE;
    public static String PrivateKeyEditor_INVALID_EDITOR_INPUT_MESSAGE;
    public static String PrivateKeyEditor_MESSAGE_DIALOG_TITLE;
    public static String PrivateKeyEditor_PROTECT_ALL_CLASSES_TITLE;
    public static String PrivateKeyEditor_PROTECT_ALL_CLASSES_TITLE_TOOLTIP;
    public static String PrivateKeyEditor_PROTECT_ALL_PACKAGES_TITLE;
    public static String PrivateKeyEditor_PROTECT_ALL_PACKAGES_TITLE_TOOLTIP;
    public static String PrivateKeyEditor_UNEXPECTED_KEY_FILE_MESSAGE;
    public static String PrivateKeyEditor_ProtectOtherKey;
    public static String PrivateKeyEditor_ProtectNoKey;
    public static String PrivateKeyEditor_ProtectPublic;
    public static String PrivateKeyEditor_ProtectNonPublic;
    public static String PrivateKeyEditor_ProtectExplictlyUnsigned;
    public static String PrivateKeyEditor_ProtectKeyTitle;
    public static String PrivateKeyEditor_Title;

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // New Project Templates
    // ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String BBTemplateSelectionPage_title;
    public static String BBTemplateSelectionPage_desc;
    public static String BBTemplateSelectionPage_selectTemplate;
    public static String BlackBerryProjectWizard_TemplateSelection;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // NEW SCREEN WIZARD
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String NewScreenWizard_title;
    public static String NewScreenWizardPage_title;
    public static String NewScreenWizardPage_description;
    public static String NewScreenWizardPage_mainScreenName;
    public static String NewScreenWizardPage_mainScreenDesc;
    public static String NewScreenWizardPage_fullScreenName;
    public static String NewScreenWizardPage_fullScreenDesc;
    public static String NewScreenWizardPage_popupScreenName;
    public static String NewScreenWizardPage_popupScreenDesc;
    public static String NewScreenWizardPage_screenType;
    public static String NewScreenWizardPage_screenTypeDesc;
    public static String NewScreenWizard_op_error_title;
    public static String NewScreenWizard_op_error_message;
    public static String NewScreenWizardPage_invalidProject;
    public static String NewScreenWizardPage_noProject;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // Update Notification
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String EJDE_FEATURE_ID;
    public static String TOOL_ID;
    public static String LATEST_VERSION;
    public static String UPGRADE_URL;
    public static String SNOOZE_DAYS_BUTTON;
    public static String IGNORE_UPDATE_BUTTON;
    public static String IGNORE_ALL_UPDATES_BUTTON;
    public static String INTERNAL_TESTING_URL;
    public static String EXTERNAL_DEVZONE_URL;
    public static String BB_PLUG_IN_URL_LABEL;
    public static String DEFAULT_DAYS;
    public static String DAY;
    public static String DAYS;
    public static String UPGRADE_NOTIFICATION_OF_EJDE_PLUGIN_TITLE;
    public static String UPGRADE_INITIALIZATION_TITLE;
    public static String UPGRADE_INITIALIZATION_LABEL;

    public static String DownloadDebugFilesDialogTitle;
    public static String DownloadDebugFilesDialogText;

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    // LOADING PROJECTS ON DEVICE
    // ///////////////////////////////////////////////////////////////////////////////////////////////
    public static String LoadProjectsOnDevice;

    public static String BlackBerryAppWorldPageURL;

    static {
        // initialize resource bundle
        NLS.initializeMessages( BUNDLE_NAME, Messages.class );
    }

    private Messages() {
    }
}
