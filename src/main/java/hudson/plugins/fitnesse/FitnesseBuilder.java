package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Execute fitnesse tests, either by starting a new fitnesse instance
 * or by using a fitnesse instance running elsewhere.
 *  
 * @author Tim Bacon
 */
public class FitnesseBuilder extends Builder {

	public static final String START_FITNESSE = "fitnesseStart";
	public static final String FITNESSE_HOST = "fitnesseHost";
	public static final String FITNESSE_PORT = "fitnessePort";
	public static final String FITNESSE_PORT_REMOTE = "fitnessePortRemote";
	public static final String FITNESSE_PORT_LOCAL = "fitnessePortLocal";
	public static final String FITNESSE_ADDITIONAL_OPTIONS = "additionalFitnesseOptions";
	public static final String JAVA_OPTS = "fitnesseJavaOpts";
	public static final String FITNESSE_JDK = "fitnesseJdk";
	public static final String PATH_TO_JAR = "fitnessePathToJar";
	public static final String PATH_TO_ROOT = "fitnessePathToRoot";
	public static final String TARGET_PAGE = "fitnesseTargetPage";
	public static final String TARGET_IS_SUITE = "fitnesseTargetIsSuite";
	public static final String PATH_TO_RESULTS = "fitnessePathToXmlResultsOut";
	public static final String HTTP_TIMEOUT = "fitnesseHttpTimeout";
	public static final String JAVA_WORKING_DIRECTORY = "fitnesseJavaWorkingDirectory";

	static final int _URL_READ_TIMEOUT_MILLIS = 60*1000;
	static final String _LOCALHOST = "localhost";
	static final String _HOSTNAME_SLAVE_PROPERTY = "HOST_NAME";
	
	private Map<String, String> options;
   private EnvVars environment;

    @DataBoundConstructor
	public FitnesseBuilder(Map<String, String> options) {
    	// Use n,v pairs to ease future extension 
    	this.options = options;
    }

    private String getOption(String key, String valueIfKeyNotFound) {
    	if (options.containsKey(key)) {
    		String value = translateEnvVar(options.get(key));
    		if (value!=null && !"".equals(value)) return value; 
    	}
    	return valueIfKeyNotFound;
    }
    
    private String translateEnvVar(String val){
       String translation = null; 
       if(null != environment && val != null && val.contains("$")){
          translation = environment.get(val.replace("$", ""));
       }
       return translation==null?val:translation;
    }
    
    //Package private scope: Expose setter to test
    void setEnvVars(EnvVars ev){
       this.environment = ev;
    }
    
    /**
     * referenced in config.jelly
     */
	public boolean getFitnesseStart() {
    	return Boolean.parseBoolean(getOption(START_FITNESSE, "False"));
    }

	/**
	 * referenced in config.jelly
	 */
    
	public String getFitnesseHost(AbstractBuild<?,?> build) throws InterruptedException, IOException  {
		if (getFitnesseStart()){
			EnvironmentVariablesNodeProperty prop = build.getBuiltOn().getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
			if (prop != null && prop.getEnvVars()!=null && prop.getEnvVars().get(_HOSTNAME_SLAVE_PROPERTY)!=null){
		  		return prop.getEnvVars().get(_HOSTNAME_SLAVE_PROPERTY);
		  	} else {
		  		return _LOCALHOST;
		  	}
		} else return getOption(FITNESSE_HOST, "unknown_host");
    }
    
	/**
    * referenced in config.jelly
    */
	 public String getFitnesseJdk() {
	      return getOption(FITNESSE_JDK, null);
	   }    
	
    /**
     * referenced in config.jelly
     */
    public String getFitnesseJavaOpts() {
    	return getOption(JAVA_OPTS, "");
    }
    
    /**
     * referenced in config.jelly
     */
    public String getFitnesseJavaWorkingDirectory() {
    	String fitnessePathToJar = getFitnessePathToJar(), fitnesseJarDir = "";
    	if (!"".equals(fitnessePathToJar)) {
    		File jarFile = new File(fitnessePathToJar);
			if (jarFile.exists()) {
    			fitnesseJarDir = jarFile.getParentFile().getAbsolutePath();
			} else { 
				fitnesseJarDir = jarFile.getParent();
				if (fitnesseJarDir == null) fitnesseJarDir = "";
    		}
    	}
    	return getOption(JAVA_WORKING_DIRECTORY, fitnesseJarDir);
    }

    /**
     * referenced in config.jelly
     */
    public int getFitnessePort() {
    	return Integer.parseInt(
			getOption(FITNESSE_PORT_REMOTE, 
				getOption(FITNESSE_PORT_LOCAL, 
					getOption(FITNESSE_PORT, "-1"))));
    }

    /**
     * referenced in config.jelly
     */
    public String getFitnessePathToJar() {
		return getOption(PATH_TO_JAR, "fitnesse.jar");
	}
    
    /**
     * referenced in config.jelly
     * Defaults to empty string
     */
    public String getAdditionalFitnesseOptions() {
    	String sanitizedOptions = getOption(FITNESSE_ADDITIONAL_OPTIONS, "");
    	// remove quotes that Jenkins config wraps around anything with a space in it
    	if(sanitizedOptions.length()>2 
    			&& sanitizedOptions.startsWith("\"") 
    			&& sanitizedOptions.endsWith("\"")){
    		sanitizedOptions = sanitizedOptions.substring(1,sanitizedOptions.length()-1);
    	}
		return sanitizedOptions;
	}

    /**
     * referenced in config.jelly
     */
    public String getFitnessePathToRoot() {
    	return getOption(PATH_TO_ROOT, "FitNesseRoot");
    }

    /**
     * referenced in config.jelly
     */
	public String getFitnesseTargetPage() {
		return getOption(TARGET_PAGE, "");
    }

	/**
	 * referenced in config.jelly
	 */
    public boolean getFitnesseTargetIsSuite() {
    	return Boolean.parseBoolean(getOption(TARGET_IS_SUITE, "False"));
    }
    
    /**
     * referenced in config.jelly
     */
    public String getFitnessePathToXmlResultsOut() {
    	return getOption(PATH_TO_RESULTS, "fitnesse-results.xml");
    }

    /**
     * referenced in config.jelly
     */
    public int getFitnesseHttpTimeout() {
    	return Integer.parseInt(getOption(HTTP_TIMEOUT, 
			String.valueOf(_URL_READ_TIMEOUT_MILLIS)));
	}
    
       /**
     * {@link Builder}
     */
    @Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
    throws IOException, InterruptedException {
      this.environment = build.getEnvironment(listener);
    	PrintStream logger = listener.getLogger();
		logger.println(getClass().getName() + ": " + resolveEnvironmentVars());
		
		FitnesseExecutor fitnesseExecutor = new FitnesseExecutor(this);
		return fitnesseExecutor.execute(build, launcher, logger, build.getEnvironment(listener));
	}

    private Map<String, String> resolveEnvironmentVars() {
       Map<String, String> translation = new HashMap<String, String>();
          for(Entry<String, String> entry :options.entrySet()){
            translation.put(entry.getKey(), translateEnvVar(entry.getValue()));
         }
          return translation;
      }

   /**
     * {@link Builder}
     */
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
    /**
     *  See <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseBuilder/config.jelly</tt>
     */
    @Extension 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	
    	public FormValidation doCheckFitnesseHost(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()==0)
    			return FormValidation.error("Please specify the host of the fitnesse instance.");
    		return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckAdditionalFitnesseOptions(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()>0){
    			if(value.contains("-r") || value.contains("-p") || value.contains("-d"))
    				return FormValidation.error("Please use the appropriate config fields to specify options for -r, -d, and -p.");
    		}
    			
    		return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckFitnessePort(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()==0)
    			return FormValidation.error("Please specify the fitnesse port.");
    		try {
    			int intValue = Integer.parseInt(value);
    			if (intValue < 1)
    				return FormValidation.error("Port must be a positive integer.");
    		} catch (NumberFormatException e) {
    			return FormValidation.error("Port must be a number.");
    		}
    		return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckFitnesseJdk(@QueryParameter String value) throws IOException, ServletException {
    	  if (value.length()==0)
           return FormValidation.ok("Defaults to project's JDK");
    	   return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckFitnesseJavaOpts(@QueryParameter String value) throws IOException, ServletException {
        	return FormValidation.ok();
        }

        public FormValidation doCheckFitnesseJavaWorkingDirectory(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.ok("Location of fitnesse.jar will be used as java working directory.");
        	if (!new File(value).exists())
        		return FormValidation.error("Path does not exist.");
        	return FormValidation.ok();
        }

        public FormValidation doCheckFitnessePathToJar(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()==0)
    			return FormValidation.error("Please specify the path to 'fitnesse.jar'.");
    		if (!value.endsWith("fitnesse.jar")
    				&& new File(value, "fitnesse.jar").exists())
    			return FormValidation.warning("Path does not end with 'fitnesse.jar': is that correct?");
    		return FormValidation.ok();
    	}

        public FormValidation doCheckFitnessePathToRoot(@QueryParameter String value) throws IOException, ServletException {
            if (value.length()==0)
                return FormValidation.error("Please specify the location of 'FitNesseRoot'.");
            if (!value.endsWith("FitNesseRoot")
            && new File(value, "FitNesseRoot").exists())
            	return FormValidation.warning("Path does not end with 'FitNesseRoot': is that correct?");
            return FormValidation.ok();
        }

        public FormValidation doCheckFitnesseTargetPage(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.error("Please specify a page to execute.");
        	return FormValidation.ok();
        }

        public FormValidation doCheckFitnesseTargetIsSuite(@QueryParameter String value) throws IOException, ServletException {
            return FormValidation.ok();
        }
        
        public FormValidation doCheckFitnesseHttpTimeout(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.ok("Default timeout " + _URL_READ_TIMEOUT_MILLIS + "ms will be used.");
        	try {
        		if (Integer.parseInt(value) < 0) return FormValidation.error("Timeout must be a positive integer.");
        	} catch (NumberFormatException e) {
        		return FormValidation.error("Timeout must be a number.");
        	}
        	return FormValidation.ok();
        }

        public FormValidation doCheckFitnessePathToXmlResultsOut(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.error("Please specify where to write fitnesse results to.");
        	if (!value.endsWith("xml"))
        		return FormValidation.warning("File does not end with 'xml': is that correct?");
        	return FormValidation.ok();
        }

        /**
         * {@link BuildStepDescriptor}
         */
        
        @SuppressWarnings("rawtypes")
      public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * {@link ModelObject} 
         */
        @Override
        public String getDisplayName() {
            return "Execute fitnesse tests";
        }

        /**
         * {@link Descriptor}
         * config.jelly uses hide-able fields so take control of instance creation
         */
		@Override
		public FitnesseBuilder newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			String startFitnesseValue = formData.getJSONObject(START_FITNESSE).getString("value");
			if (Boolean.parseBoolean(startFitnesseValue)) {
				return newFitnesseBuilder(startFitnesseValue, 
						collectFormData(formData, new String[] {
							FITNESSE_JDK, JAVA_OPTS, JAVA_WORKING_DIRECTORY,
							PATH_TO_JAR, PATH_TO_ROOT, FITNESSE_PORT_LOCAL, 
							TARGET_PAGE, TARGET_IS_SUITE, HTTP_TIMEOUT, PATH_TO_RESULTS, 
							FITNESSE_ADDITIONAL_OPTIONS
						})
				);
			}
			return newFitnesseBuilder(startFitnesseValue, 
					collectFormData(formData, new String[] {
						FITNESSE_HOST, FITNESSE_PORT_REMOTE,  
						TARGET_PAGE, TARGET_IS_SUITE, HTTP_TIMEOUT, PATH_TO_RESULTS
					})
			);
		}

		private FitnesseBuilder newFitnesseBuilder(String startFitnesseValue, Map<String, String> collectedFormData) {
			collectedFormData.put(START_FITNESSE, startFitnesseValue);
			return new FitnesseBuilder(collectedFormData);
		}

		private Map<String, String> collectFormData(JSONObject formData, String[] keys) {
			Map<String, String> targetElements = new HashMap<String, String>();
			for (String key: keys) {
				if (formData.has(key)) {
					targetElements.put(key, formData.getString(key));
				} else {
					targetElements.put(key, 
						formData.getJSONObject(START_FITNESSE).getString(key));
				}
			}
			return targetElements;
		}
    }
}

