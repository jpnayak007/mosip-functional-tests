package io.mosip.resident.tests;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.authentication.fw.dto.OutputValidationDto;
import io.mosip.authentication.fw.precon.JsonPrecondtion;
import io.mosip.authentication.fw.util.AuditValidation;
import io.mosip.authentication.fw.util.AuthenticationTestException;
import io.mosip.authentication.fw.util.DataProviderClass;
import io.mosip.authentication.fw.util.FileUtil;
import io.mosip.authentication.fw.util.OutputValidationUtil;
import io.mosip.authentication.fw.util.ReportUtil;
import io.mosip.authentication.fw.util.RunConfigUtil;
import io.mosip.authentication.fw.util.StoreAuthenticationAppLogs;
import io.mosip.authentication.fw.util.TestParameters;
import io.mosip.authentication.testdata.TestDataProcessor;
import io.mosip.authentication.testdata.TestDataUtil;
import io.mosip.authentication.tests.OtpAuthentication;
import io.mosip.resident.fw.util.ResidentTestUtil;
import io.mosip.util.Cookie;

/**
 * Functional Test for Revoke VID - Resident Service
 * 
 * @author Vignesh
 *
 */
public class RevokeVID extends ResidentTestUtil implements ITest{
	
	private static final Logger logger = Logger.getLogger(RevokeVID.class);
	protected static String testCaseName = "";
	private String TESTDATA_PATH;
	private String TESTDATA_FILENAME;
	private String testType;
	private int invocationCount = 0;
	private static String cookieValue;
	private static String residentCookieValue;
	
	/**
	 * Set Test Type - Smoke, Regression or Integration
	 * 
	 * @param testType
	 */
	@BeforeClass
	public void setTestTypeAndDbConnection() {
		this.testType = RunConfigUtil.getTestLevel();
	}
	
	public void setCookie() {
		cookieValue = getAuthorizationCookie(getCookieRequestFilePath(),
				RunConfigUtil.objRunConfig.getEndPointUrl() + RunConfigUtil.objRunConfig.getClientidsecretkey(),
				AUTHORIZATHION_COOKIENAME);
	}
	
	public void getResidentAccess() {
		residentCookieValue = getAuthorizationCookie(getCookieRequestFilePathForResidentAuth(),
				RunConfigUtil.objRunConfig.getEndPointUrl() + RunConfigUtil.objRunConfig.getClientidsecretkey(),
				AUTHORIZATHION_COOKIENAME);
	}

	/**
	 * Method set Test data path and its filename
	 * 
	 * @param index
	 */
	public void setTestDataPathsAndFileNames(int index) {
		this.TESTDATA_PATH = getTestDataPath(this.getClass().getSimpleName().toString(), index);
		this.TESTDATA_FILENAME = getTestDataFileName(this.getClass().getSimpleName().toString(), index);
	}

	/**
	 * Method set configuration
	 * 
	 * @param testType
	 */
	public void setConfigurations(String testType) {
		RunConfigUtil.getRunConfigObject("resident");
		RunConfigUtil.objRunConfig.setConfig(this.TESTDATA_PATH, this.TESTDATA_FILENAME, testType);
		TestDataProcessor.initateTestDataProcess(this.TESTDATA_FILENAME, this.TESTDATA_PATH, "resident");
	}

	/**
	 * The method set test case name
	 * 
	 * @param method
	 * @param testData
	 */
	@BeforeMethod
	public void testData(Method method, Object[] testData) {
		String testCase = "";
		if (testData != null && testData.length > 0) {
			TestParameters testParams = null;
			// Check if test method has actually received required parameters
			for (Object testParameter : testData) {
				if (testParameter instanceof TestParameters) {
					testParams = (TestParameters) testParameter;
					break;
				}
			}
			if (testParams != null) {
				testCase = testParams.getTestCaseName();
			}
		}
		this.testCaseName = String.format(testCase);
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[][] getTestCaseList() {
		invocationCount++;
		setTestDataPathsAndFileNames(invocationCount);
		setConfigurations(this.testType);
		return DataProviderClass.getDataProvider(
				RunConfigUtil.getResourcePath() + RunConfigUtil.objRunConfig.getScenarioPath(),
				RunConfigUtil.objRunConfig.getScenarioPath(), RunConfigUtil.objRunConfig.getTestType());
	}

	/**
	 * Set current testcaseName
	 */
	@Override
	public String getTestName() {
		return this.testCaseName;
	}

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, RevokeVID.testCaseName);
			if(!result.isSuccess())
				StoreAuthenticationAppLogs.storeApplicationLog(RunConfigUtil.getAuthSeriveName(), logFileName, getTestFolder());
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}

	/**
	 * Test method for OTP authentication execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException 
	 */
	@Test(dataProvider = "testcaselist")
	public void residentRevokeVID(TestParameters objTestParameters, String testScenario, String testcaseName) throws AuthenticationTestException {
		setCookie();
		getResidentAccess();
		File testCaseName = objTestParameters.getTestCaseFile();
		int testCaseNumber = Integer.parseInt(objTestParameters.getTestId());
		displayLog(testCaseName, testCaseNumber);
		setTestFolder(testCaseName);
		setTestCaseId(testCaseNumber);
		setTestCaseName(testCaseName.getName());
		String mapping = TestDataUtil.getMappingPath();
		logger.info("*************Otp generation request ******************");
		Reporter.log("<b><u>Otp generation request</u></b>");
		displayContentInFile(testCaseName.listFiles(), "otp-generate");
		logger.info("******Post request Json to EndPointUrl: " + RunConfigUtil.objRunConfig.getEndPointUrl() + RunConfigUtil.objRunConfig.getIdaInternalOtpPath()
				+ " *******");
		if(!postRequestAndGenerateOuputFileForIntenalAuth(testCaseName.listFiles(),
				RunConfigUtil.objRunConfig.getEndPointUrl() + RunConfigUtil.objRunConfig.getIdaInternalOtpPath(), "otp-generate", "output-1-actual-res",
				AUTHORIZATHION_COOKIENAME, residentCookieValue, 200))
			throw new AuthenticationTestException("Failed at HTTP-POST otp-generate-request");
		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doOutputValidation(
				FileUtil.getFilePath(testCaseName, "output-1-actual").toString(),
				FileUtil.getFilePath(testCaseName, "output-1-expected").toString());
		Reporter.log(ReportUtil.getOutputValiReport(ouputValid));
		if(!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AuthenticationTestException("Failed at otp-generate-response output validation");
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("pinInfovalue", getOtpValue(
				FileUtil.getFilePath(testCaseName, "revoke-vid-request").getAbsolutePath(), mapping, "pinInfovalue"));
		Reporter.log("<b><u>Modification of otp value in revoke-vid-request</u></b>");
		if(!modifyRequest(testCaseName.listFiles(), tempMap, mapping, "revoke-vid-request"))
			throw new AuthenticationTestException("Failed at modifying the otp value in revoke-vid-request file. Kindly check testdata.");
		displayContentInFile(testCaseName.listFiles(), "revoke-vid-request");
		String vid = JsonPrecondtion.getJsonValueFromJson(FileUtil.readInput(FileUtil.getFilePath(testCaseName, "revoke-vid-request").getAbsolutePath()), "request.individualId");
		vid=vid.replace("\"", "");
		String revokeVidUrl = RunConfigUtil.objRunConfig.getResidentRevokeVID().replace("$vid$", vid);
		logger.info("******Post request Json to EndPointUrl: " + RunConfigUtil.objRunConfig.getEndPointUrl() + revokeVidUrl				+ " *******");
		
		if (!patchRequestAndGenerateOuputFileForIntenalAuth(testCaseName.listFiles(),
				RunConfigUtil.objRunConfig.getEndPointUrl() + revokeVidUrl,
				"revoke-vid-request", "output-2-actual-res", AUTHORIZATHION_COOKIENAME, residentCookieValue, 200))
			throw new AuthenticationTestException("Failed at HTTP-POST revoke-vid-request");
		Map<String, List<OutputValidationDto>> ouputValid2 = OutputValidationUtil.doOutputValidation(
				FileUtil.getFilePath(testCaseName, "output-2-actual").toString(),
				FileUtil.getFilePath(testCaseName, "output-2-expected").toString());
		Reporter.log(ReportUtil.getOutputValiReport(ouputValid2));
		if(!OutputValidationUtil.publishOutputResult(ouputValid2))
			throw new AuthenticationTestException("Failed at revoke-vid-request output validation");
	}

}
