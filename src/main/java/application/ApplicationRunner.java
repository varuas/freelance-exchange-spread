package application;

import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import application.configuration.AppConfig;

/**
 * Starts the application.
 */
public class ApplicationRunner {

	/**
	 * JSON configuration file that stores the parameters required for this application
	 */
	private static final String CONFIGURATION_FILE_NAME = "config.json";

	private static Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

	public static void main(String args[]) throws Exception {

		LOGGER.info("Application started...");

		final ObjectMapper objMapper = new ObjectMapper();
		final InputStream configResource = ApplicationRunner.class.getResourceAsStream(CONFIGURATION_FILE_NAME);
		final AppConfig appConfig = objMapper.readValue(configResource, AppConfig.class);

		final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		final SpreadCalculatorTask spreadCalculatorTask = new SpreadCalculatorTask(appConfig);
		executor.scheduleWithFixedDelay(spreadCalculatorTask, 0, appConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);

		System.out.println("Program started...\nTo exit, press Ctrl-C");
		System.in.read();
		executor.shutdownNow();
		executor.awaitTermination(2, TimeUnit.SECONDS);

		LOGGER.info("Application stopped");
	}
}