package application;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.configuration.AppConfig;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;

public class Utils {

	private static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	public static Map<String, BaseExchangeConnector> createExchangeConnectorInstances(AppConfig appConfig) {
		final Map<String, BaseExchangeConnector> connectors = new HashMap<>(appConfig.getExchanges().size());
		for (final ExchangeConfig exchangeConfig : appConfig.getExchanges()) {
			final String id = exchangeConfig.getId();
			final String connectorClass = exchangeConfig.getConnectorClass();
			try {
				@SuppressWarnings("unchecked")
				final Constructor<BaseExchangeConnector> constructor = (Constructor<BaseExchangeConnector>) Class
						.forName(connectorClass).getConstructor(ExchangeConfig.class);
				final BaseExchangeConnector connector = constructor.newInstance(exchangeConfig);
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
	 * Formats the given SpreadInfo array into a string suitable for console printing.
	 *
	 * Unfortunately, the observable zip API returns an object array.
	 * Expectation is that the 'spreadArr' param should be of type SpreadInfo[];
	 */
	public static String formatConsolePrint(List<SpreadInfo> spreadInfos) {
		final StringBuilder outputBuilder = new StringBuilder();
		outputBuilder.append("\nSpread print time : " + LocalDateTime.now() + "\n");
		outputBuilder.append("--------------------------------------------------------------------------------\n");
		outputBuilder.append(String.format("%4s%8s%9s%12s%15s%12s%15s\n", "Sr", "Spread%", "Currency", "Ask", "Ask-Exch", "Bid", "Bid-Exch"));
		outputBuilder.append("--------------------------------------------------------------------------------\n");
		int index = 1;
		for(final SpreadInfo spread : spreadInfos) {
			outputBuilder.append(String.format(
					"%4d"    // Sr.
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
