package hudson.plugins.fitnesse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.DescribableList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseBuilderTest {
   @Test
   public void getJdkShouldReturnSpecificJavaHomeIfSpecified() {
      HashMap<String, String> options = new HashMap<String, String>();
      String expectedJavaHome = "jdk1.6.0_18";
      options.put(FitnesseBuilder.FITNESSE_JDK, expectedJavaHome);
      FitnesseBuilder builder = new FitnesseBuilder(options);
      Assert.assertEquals(expectedJavaHome, builder.getFitnesseJdk());
   }
   
   @Test
   public void getJdkShouldReturnNothingIfNotSpecifiedSoThatTheDefaultJDKIsUsed() {
      HashMap<String, String> options = new HashMap<String, String>();
      String expectedJavaHome = null;
      FitnesseBuilder builder = new FitnesseBuilder(options);
      
      Assert.assertEquals(expectedJavaHome, builder.getFitnesseJdk());
   }
   
	@Test
	public void getPortShouldReturnLocalPortIfSpecified() {
	   
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "99");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(99, builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, null);
		Assert.assertEquals(99, builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "");
		Assert.assertEquals(99, builder.getFitnessePort());
	}
	
	@Test
	public void getPortShouldReturnResolvedEnvironmentVariableIfSpecified() {
	   HashMap<String, String> options = new HashMap<String, String>();
	   options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "$port");
	   FitnesseBuilder builder = new FitnesseBuilder(options);
	   EnvVars ev = new EnvVars();
      ev.put("port", "99");
      builder.setEnvVars(ev);
	   Assert.assertEquals(99, builder.getFitnessePort());
	   
	   options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, null);
	   Assert.assertEquals(99, builder.getFitnessePort());
	   
	   options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "");
	   Assert.assertEquals(99, builder.getFitnessePort());
	}
	
	@Test
	public void getPortShouldReturnRemotePortIfSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "999");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(999, builder.getFitnessePort());
		
		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, null);
		Assert.assertEquals(999, builder.getFitnessePort());
		
		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "");
		Assert.assertEquals(999, builder.getFitnessePort());
	}
	
   @Test
	public void getHostShouldReturnLocalHostIfStartBuildIsTrue() throws InterruptedException, IOException {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.START_FITNESSE, "True");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		
		Assert.assertTrue(builder.getFitnesseStart());
		
		AbstractBuild<?, ?> build = mockBuildWithEnvVars(null);
      
      Assert.assertEquals("localhost", builder.getFitnesseHost(build ));
		
		options.put(FitnesseBuilder.FITNESSE_HOST, "abracadabra");
		Assert.assertEquals("localhost", builder.getFitnesseHost(build));
	}

   @SuppressWarnings({ "rawtypes", "unchecked" })
   private AbstractBuild<?, ?> mockBuildWithEnvVars(EnvVars envVars) {
      AbstractBuild<?, ?> build = mock(AbstractBuild.class);
		Node mockNode = mock(Node.class);
		DescribableList nodeProperties = mock(DescribableList.class);
		EnvironmentVariablesNodeProperty envVarsNodeProperty = mock(EnvironmentVariablesNodeProperty.class);
		
		when(envVarsNodeProperty.getEnvVars()).thenReturn(envVars);
      when(nodeProperties.get(EnvironmentVariablesNodeProperty.class)).thenReturn(envVarsNodeProperty);
      when(mockNode.getNodeProperties()).thenReturn(nodeProperties);
      when(build.getBuiltOn()).thenReturn(mockNode);
      return build;
   }
	
	@Test
	public void getHostShouldReturnSpecifiedHostIfStartBuildIsFalse() throws InterruptedException, IOException {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.START_FITNESSE, "False");
		options.put(FitnesseBuilder.FITNESSE_HOST, "hudson.local");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		
		AbstractBuild<?, ?> build = mockBuildWithEnvVars(null);
		
		Assert.assertFalse(builder.getFitnesseStart());
		Assert.assertEquals("hudson.local", builder.getFitnesseHost(build));
		
		options.put(FitnesseBuilder.FITNESSE_HOST, "abracadabra");
		Assert.assertEquals("abracadabra", builder.getFitnesseHost(build));
	}
	
	@Test
	public void getHostShouldResolveEnvironmentVariablesIfStartBuildIsFalse() throws InterruptedException, IOException {
	   EnvVars envVars = new EnvVars();
	   envVars.put("host", "abracadabra");

	   HashMap<String, String> options = new HashMap<String, String>();
	   options.put(FitnesseBuilder.START_FITNESSE, "False");
	   options.put(FitnesseBuilder.FITNESSE_HOST, "$host");
	   FitnesseBuilder builder = new FitnesseBuilder(options);
	   builder.setEnvVars(envVars);
	   
      AbstractBuild<?, ?> build = mockBuildWithEnvVars(null);
	   
	   Assert.assertFalse(builder.getFitnesseStart());
	   Assert.assertEquals("abracadabra", builder.getFitnesseHost(build));
	}

	@Test
	public void getHttpTimeoutShouldReturn60000UnlessValueIsExplicit() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(60000, builder.getFitnesseHttpTimeout());
		options.put(FitnesseBuilder.HTTP_TIMEOUT, "1000");
		Assert.assertEquals(1000, builder.getFitnesseHttpTimeout());
	}
	
	@Test
	public void getHttpTimeoutShouldResolveEnvironmentVariables() {
	   HashMap<String, String> options = new HashMap<String, String>();
	   FitnesseBuilder builder = new FitnesseBuilder(options);
	   EnvVars ev = new EnvVars();
      ev.put("timeout", "1000");
      builder.setEnvVars(ev);
	   Assert.assertEquals(60000, builder.getFitnesseHttpTimeout());
	   
	   options.put(FitnesseBuilder.HTTP_TIMEOUT, "$timeout");
	   Assert.assertEquals(1000, builder.getFitnesseHttpTimeout());
	}
	
	@Test
	public void getJavaWorkingDirShouldReturnParentOfFitnessseJarUnlessValueIsExplicit() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		File tmpFile = File.createTempFile("fitnesse", ".jar");
		options.put(FitnesseBuilder.PATH_TO_JAR, tmpFile.getAbsolutePath());
		
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(tmpFile.getParentFile().getAbsolutePath(), 
				builder.getFitnesseJavaWorkingDirectory());
		
		options.put(FitnesseBuilder.JAVA_WORKING_DIRECTORY, "/some/explicit/path");
		Assert.assertEquals("/some/explicit/path", builder.getFitnesseJavaWorkingDirectory());
	}	
	
	@Test
	public void getJavaWorkingDirShouldReturnParentOfFitnessseJarEvenIfRelativeToBuildDir() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		File tmpFile = new File("relativePath", "fitnesse.jar");
		options.put(FitnesseBuilder.PATH_TO_JAR, tmpFile.getPath());
		
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals("relativePath", builder.getFitnesseJavaWorkingDirectory());
	}
	
	@Test
	public void getJavaWorkingDirShouldBeEmptyIfFitnessseJarUnspecified() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals("", 
				builder.getFitnesseJavaWorkingDirectory());
	}
}
