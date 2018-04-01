package application;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import application.configuration.AppConfig;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

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

		final SpreadCalculatorTask task = new SpreadCalculatorTask(appConfig);
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Scheduler scheduler = Schedulers.from(executor);
		Observable
			.interval(appConfig.getRefreshInterval(), TimeUnit.MILLISECONDS)
			.subscribeOn(scheduler)
			.observeOn(scheduler)
			.subscribe(tick -> task.run());

		System.out.println("Program started...\nTo exit, press Ctrl-C");
		System.in.read();

		LOGGER.info("Application stopped");
		System.out.println("Program terminated.");
		System.exit(0);
	}
}