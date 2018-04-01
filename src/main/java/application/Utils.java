package application;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import application.configuration.AppConfig;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Common utility class for the application
 */
public class Utils {

	/**
	 * Logger instance for this class
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	/**
	 * Dynamically creates connector instances from the class names given in 'config.json'.
	 */
	public static Map<String, BaseExchangeConnector> createExchangeConnectorInstances(AppConfig appConfig) {

		final Map<String, Integer> threadPoolConfigs = appConfig.getThreadPools();
		final Map<String, Scheduler> schedulers = Maps.newHashMapWithExpectedSize(threadPoolConfigs.keySet().size());

		threadPoolConfigs.forEach((poolId, poolSize) -> {
			final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
			final Scheduler scheduler = Schedulers.from(executor);
			schedulers.put(poolId, scheduler);
		});

		final Map<String, BaseExchangeConnector> connectors = new HashMap<>(appConfig.getExchanges().size());
		for (final ExchangeConfig exchangeConfig : appConfig.getExchanges()) {
			final String id = exchangeConfig.getId();
			final String poolId = exchangeConfig.getThreadPool();
			final Scheduler scheduler = schedulers.get(poolId);
			final String connectorClass = exchangeConfig.getConnectorClass();
			try {
				@SuppressWarnings("unchecked")
				final Constructor<BaseExchangeConnector> constructor = (Constructor<BaseExchangeConnector>) Class
						.forName(connectorClass).getConstructor(AppConfig.class, ExchangeConfig.class, Scheduler.class);
				final BaseExchangeConnector connector = constructor.newInstance(appConfig, exchangeConfig, scheduler);
				connectors.put(id, connector);
				LOGGER.info("Initialized connector : " + connectorClass);
			} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
				final String errorMsg = "Failed to initialize exchange connector : " + connectorClass;
				LOGGER.error(errorMsg, e);
				throw new IllegalArgumentException(errorMsg, e);
			}
		}
		return connectors;
	}

	/**
	 * Formats the given SpreadInfo list into a string suitable for console
	 * printing.
	 */
	public static String formatConsolePrint(List<SpreadInfo> spreadInfos) {
		final StringBuilder outputBuilder = new StringBuilder();

		outputBuilder.append("\nTime : " + LocalDateTime.now() + "\n");
		outputBuilder.append("--------------------------------------------------------------------------------\n");

		if(spreadInfos.isEmpty()) {
			outputBuilder.append("Initializing...\n");
			return outputBuilder.toString();
		}

		outputBuilder.append(String.format("%4s%8s%9s%12s%15s%12s%15s\n",
				"Sr", "Spread%", "Currency", "Ask", "Ask-Exch", "Bid", "Bid-Exch"));
		outputBuilder.append("--------------------------------------------------------------------------------\n");
		int index = 1;
		for(final SpreadInfo spread : spreadInfos) {
			outputBuilder.append(String.format(
					"%4d"    // Sr. No.
					+ "%8.4f" // Spread
					+ "%9s" // Currency
					+ "%12.4f" // Ask
					+ "%15s" // Ask-Exchange
					+ "%12.4f" // Bid
					+ "%15s" // Bid-Exchange
					+ "\n",
					index++, spread.getSpreadPercent(), spread.getCcyPair(), spread.getBestAskPrice(),
					spread.getBestAskExchange(), spread.getBestBidPrice(), spread.getBestBidExchange()));
		}
		return outputBuilder.toString();
	}
}
