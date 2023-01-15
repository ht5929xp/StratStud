package com.op.bo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RobotLogsReader {

	private static final String ROBOT_LOG_DELIMETER = "\t";
	private static final String ORDER_DELIMETER = ",";
	
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat df2 = new SimpleDateFormat("M/d/yyyy h:m:s a");
	private static final SimpleDateFormat df3 = new SimpleDateFormat("MMddyyyy");
	
	private static final String ORDER_CREATED_CATEGORY = "N/A";
	private static final String ORDER_CREATED = "Got OrderCreated";
	
	private static final String SUBMIT_ORDERS_CATEGORY = "IBClient";
	private static final String SUBMIT_ORDERS = "[SubmitOrders]";
	
	private static final String ORDER_STATUS_CATEGORY = "fileonly";
	private static final String ORDER_STATUS = "[orderStatus]";
	
	private static final String OPEN_ORDER_CATEGORY = "fileonly";
	private static final String OPEN_ORDER = "[openOrder]";
	
	private static final String ORDER_UPDATED_CATEGORY = "fileonly";
	private static final String ORDER_UPDATED = "Got OrderUpdated";

	private static final String ORDER_CANCELLED_STATUS = "Canceled";
	private static final String ORDER_FILLED_STATUS = "Filled";
	
	private static final Map<String, Order> ordersMap = new HashMap<>();
	private static final List<Order> orders = new ArrayList<>();
	
	public static PerformanceData readLogData() {
		PerformanceData performanceData = new PerformanceData();

		File robotLogsDir = new File(PathResources.ROBOT_LOGS_PATH);

		for (File log : robotLogsDir.listFiles()) {
			if (log.getName().contains("log")) {
				try (BufferedReader reader = new BufferedReader(new FileReader(log))) {

					Map<String, Order> tempMap = new LinkedHashMap<>();
					
					String line = null;
					while ((line = reader.readLine()) != null) {
						String[] array = line.split(ROBOT_LOG_DELIMETER);
						
						Order order = null;
						
						if (array.length > 2) {
							String timestampStr = array[0];
							String categoryStr = array[1];
							String dataStr = array[2];
							if (categoryStr.equals(ORDER_CREATED_CATEGORY) && dataStr.startsWith(ORDER_CREATED)) {
								final String find = "BrokerInterfaceIB.IBManager for order: ";
								
								try {
									String orderStr = dataStr.substring(dataStr.indexOf(find)).trim();
									orderStr = orderStr.replace(find, "");
									order = getOrderData(orderStr);

									Date timestamp = getTimestamp(timestampStr);
									setOrderStatusTimestamps(order, timestamp);
								} catch (StringIndexOutOfBoundsException e) {
									System.out.println("ERROR: " + line);
								}
							} else if (categoryStr.equals(SUBMIT_ORDERS_CATEGORY) && dataStr.startsWith(SUBMIT_ORDERS)) {
								final String find = "[SubmitOrders] sending order for ";
								String orderStr = dataStr.substring(dataStr.indexOf(find)).trim();
								orderStr = "symbol=" + orderStr.replace(find, "");
								order = getOrderData(orderStr);
								
								Date timestamp = getTimestamp(timestampStr);
								setOrderStatusTimestamps(order, timestamp);
							} else if (categoryStr.equals(ORDER_STATUS_CATEGORY) && dataStr.startsWith(ORDER_STATUS)) {
								final String find = "[orderStatus] ";
								String orderStr = dataStr.substring(dataStr.indexOf(find)).trim();
								orderStr = orderStr.replace(find, "");
								order = getOrderData(orderStr);
								
								Date timestamp = getTimestamp(timestampStr);
								setOrderStatusTimestamps(order, timestamp);
							} else if (categoryStr.equals(OPEN_ORDER_CATEGORY) && dataStr.startsWith(OPEN_ORDER)) {
								final String find = "[openOrder] ";
								String orderStr = dataStr.substring(dataStr.indexOf(find)).trim();
								orderStr = orderStr.replace(find, "");
								order = getOrderData(orderStr);
								
								Date timestamp = getTimestamp(timestampStr);
								setOrderStatusTimestamps(order, timestamp);
							} else if (categoryStr.equals(ORDER_UPDATED_CATEGORY) && dataStr.startsWith(ORDER_UPDATED)) {
								final String find = "BrokerInterfaceIB.IBManager for order: ";
	
								try {
									String orderStr = dataStr.substring(dataStr.indexOf(find)).trim();
									orderStr = orderStr.replace(find, "");
									order = getOrderData(orderStr);
									
									Date timestamp = getTimestamp(timestampStr);
									setOrderStatusTimestamps(order, timestamp);
								} catch (StringIndexOutOfBoundsException e) {
									System.out.println("ERROR: " + line);
								}
							}
							
							if (order != null) {
								String key = df3.format(order.getSubmittedTs()) + "-" + String.valueOf(order.getOrderId());
								if (tempMap.containsKey(key)) {
									updateOrder(tempMap.get(key), order);
								} else {
									tempMap.put(key, order);
									orders.add(order);
								}
							}
						}
					}
					
					for (Order o : tempMap.values()) {
						ordersMap.put(o.getOrderId() + "-" + o.getSymbol(), o);
					}
					
				} catch (IOException e) {
					System.err.println("Error reading log file: " + log.getName());
				}
			}
		}
		
		cleanOrderData();

		for (Order o : orders) {
			System.out.println(o);
		}

		performanceData.setOrders(orders);
		performanceData.setTrades(buildTrades(orders));
		
		return performanceData;
	}

	private static List<Trade> buildTrades(List<Order> orders) {
		List<Trade> trades = new ArrayList<>();
		
		Map<String, List<Order>> stratSymbolMap = new HashMap<>();

		Collections.sort(orders, (a, b) -> a.getSubmittedTs().compareTo(b.getSubmittedTs()));

		for (Order o : orders) {
			String key = o.getStrategy() + ":" + o.getSymbol();

			if (stratSymbolMap.containsKey(key)) {
				stratSymbolMap.get(key).add(o);
			} else {
				List<Order> list = new ArrayList<>();
				list.add(o);
				stratSymbolMap.put(key, list);
			}
		}
		
		for (List<Order> oList : stratSymbolMap.values()) {
			
			String symbol = null;
			String strategy = null;
			
			Trade t = new Trade();
			
			for (Order o : oList) {
				if (symbol == null) {
					symbol = o.getSymbol();
				}
				if (strategy == null) {
					strategy = o.getStrategy();
				}
				if(symbol.equals("AREC")) {
					System.out.println(t.getFilledShares() + "    " + t.getSymbol() + "   " + o.getParentId() + "    " + o.getSubmittedTs() + "    " + t.getEntryDate());
				}
				
				if (t.getFilledShares() == 0 && t.getSymbol() != null && (o.getParentId() == null || o.getParentId() == 0)
						&& !df3.format(o.getSubmittedTs()).equals(df3.format(t.getEntryDate()))) {
					trades.add(t);
					t = new Trade();
				}
				
				if (t.getSymbol() == null && t.getStrategy() == null && (o.getParentId() == null || o.getParentId() == 0)) {
					t.setEntryDate(o.getFilledTs() != null ? o.getFilledTs()
							: o.getPartialFilledTs() != null ? o.getPartialFilledTs() : o.getSubmittedTs());
					t.setFilledShares(o.getFilled() == null ? 0 : o.getFilled());
					t.setEntryPrice(o.getAvgFillPrice());
					t.setSymbol(symbol);
					t.setStrategy(strategy);
					t.setEntryOrderType(o.getType());
					t.setEntryLimit(o.getLimit());
					
					if (o.getBuy()) {
						t.setBuy(o);
						t.setShort(false);
					} else {
						t.setSell(o);
						t.setShort(true);
					}
				} else {
					if (!o.getBuy() && !t.isShort()) {
						
						if (o.getFilled() != null && o.getFilled() > 0) {
							if (t.getExitShares() > 0) {
								double newAvgFill = (t.getExitPrice() * t.getExitShares()
										+ o.getFilled() * o.getAvgFillPrice())
										/ (o.getFilled() + t.getExitShares());

								t.setExitShares(t.getExitShares() + o.getFilled());
								t.setExitPrice(newAvgFill);
							} else {
								t.setExitPrice(o.getAvgFillPrice());
							}
							
							t.setExitDate(o.getFilledTs() != null ? o.getFilledTs() : o.getPartialFilledTs());
						}
						
						if ("Market".equalsIgnoreCase(o.getType())) {
							t.setTimeout(o);
							t.setTimeoutLimitPrice(o.getLimit());
							t.setTimeoutOrderType(o.getType());
						} else if (o.getLimit() != null && o.getLimit() > 0.0 && o.getStop() != null
								&& o.getStop() > 0.0) {
							t.setStopLimitPrice(o.getLimit());
							t.setStopOrderType("StopLimit");
							t.setStopPrice(o.getStop());
							t.setStopLoss(o);
						} else if (o.getStop() != null && o.getStop() > 0.0) {
							t.setStopOrderType("Stop");
							t.setStopPrice(o.getStop());
							t.setStopLoss(o);
						} else if (o.getLimit() != null && o.getLimit() > 0.0 && t.getTargetOrderType() == null) {
							t.setTargetOrderType(o.getType());
							t.setTargetLimitPrice(o.getLimit());
							t.setTarget(o);
						}
					}
				}
				
				if (t.getFilledShares() == t.getExitShares() && t.getFilledShares() != 0) {
					break;
				}
			}
			
			trades.add(t);
		}
		
		return trades;
	}
	
	private static void cleanOrderData() {
		for (Iterator<Order> orderIter = orders.iterator(); orderIter.hasNext();) {
			Order o = orderIter.next();
			if (o.getStrategy() == null || o.getStrategy().trim().equals("")) {
				String key = o.getOrderId() + o.getSymbol();
				orderIter.remove();
				ordersMap.remove(key);
			}
		}
	}
	
	private static void updateOrder(Order mainOrder, Order newOrder) {
		if (newOrder.getAvgFillPrice() != null) {
			mainOrder.setAvgFillPrice(newOrder.getAvgFillPrice());
		}
		if (newOrder.getBuy() != null) {
			mainOrder.setBuy(newOrder.getBuy());
		}
		if (newOrder.getFilled() != null) {
			
			int shares = mainOrder.getShares() == null || mainOrder.getShares() == 0 ? newOrder.getShares() : mainOrder.getShares();

			if (mainOrder.getPartialFilledTs() == null && (mainOrder.getFilled() == null
					|| (newOrder.getFilled() > mainOrder.getFilled() && newOrder.getFilled() < shares))) {
				mainOrder.setPartialFilledTs(newOrder.getSubmittedTs());
			}

			mainOrder.setFilled(newOrder.getFilled());
		}
		if (newOrder.getGtd() != null) {
			mainOrder.setGtd(newOrder.getGtd());
		}
		if (newOrder.getLastFillPrice() != null) {
			mainOrder.setLastFillPrice(newOrder.getLastFillPrice());
		}
		if (newOrder.getLimit() != null) {
			mainOrder.setLimit(newOrder.getLimit());
		}
		if (newOrder.getOcaGroup() != null) {
			mainOrder.setOcaGroup(newOrder.getOcaGroup());
		}
		if (newOrder.getParentId() != null) {
			mainOrder.setParentId(newOrder.getParentId());
		}
		if (newOrder.getPosition() != null) {
			mainOrder.setPosition(newOrder.getPosition());
		}
		if (newOrder.getRemaining() != null) {
			mainOrder.setRemaining(newOrder.getRemaining());
		}
		if (newOrder.getShares() != null && newOrder.getShares() != 0) {
			mainOrder.setShares(newOrder.getShares());
		}
		if (newOrder.getStatus() != null) {
			mainOrder.setStatus(newOrder.getStatus());
		}
		if (newOrder.getStop() != null) {
			mainOrder.setStop(newOrder.getStop());
		}
		if (newOrder.getStrategy() != null) {
			mainOrder.setStrategy(newOrder.getStrategy());
		}
		if (newOrder.getSymbol() != null) {
			mainOrder.setSymbol(newOrder.getSymbol());
		}
		if (newOrder.getType() != null) {
			mainOrder.setType(newOrder.getType());
		}
		if (newOrder.getCancelledTs() != null && mainOrder.getCancelledTs() == null) {
			mainOrder.setCancelledTs(newOrder.getCancelledTs());
		}
		if (newOrder.getSubmittedTs() != null && mainOrder.getSubmittedTs() == null) {
			mainOrder.setSubmittedTs(newOrder.getSubmittedTs());
		}
		if (newOrder.getFilledTs() != null && mainOrder.getFilledTs() == null) {
			mainOrder.setFilledTs(newOrder.getFilledTs());
		}
	}

	private static void setOrderStatusTimestamps(Order order, Date timestamp) {
		order.setSubmittedTs(timestamp);
		if (order.getStatus() != null) {
			if (order.getStatus().equals(ORDER_FILLED_STATUS)) {
				order.setFilledTs(timestamp);
			} else if (order.getStatus().equals(ORDER_CANCELLED_STATUS)) {
				order.setCancelledTs(timestamp);
			}
		}
	}
	
	private static Date getTimestamp(String timestampStr) {
		Date stamp = null;
		try {
			stamp = df.parse(timestampStr.substring(0, 19));

			Calendar cal = Calendar.getInstance();
			cal.setTime(stamp);
			int hour = cal.get(Calendar.HOUR);
			if (hour < 4) {
				cal.set(Calendar.AM_PM, Calendar.PM);
			} else {
				cal.set(Calendar.AM_PM, Calendar.AM);
			}
			stamp = cal.getTime();
		} catch (ParseException e) {
			System.err.println("Error parsing date: " + timestampStr);
		}
		
		return stamp;
	}

	private static Order getOrderData(String orderStr) {
		Order order = new Order();
		
		String[] orderArray = orderStr.split(ORDER_DELIMETER);
		for (String elem : orderArray) {
			elem = elem.trim();
			String dataStr = elem.substring(elem.indexOf("=") + 1);
			if (dataStr.length() > 0) {
				if (elem.toUpperCase().startsWith("SYMBOL=")) {
					String symbol = dataStr;
					order.setSymbol(symbol);
				} else if (elem.toUpperCase().startsWith("ORDERID=")) {
					int orderId = Integer.parseInt(dataStr);
					order.setOrderId(orderId);
				} else if (elem.toUpperCase().startsWith("STATUS=")) {
					String status = dataStr;
					order.setStatus(status);
				} else if (elem.toUpperCase().startsWith("SHARES=")) {
					int shares = Integer.parseInt(dataStr);
					order.setShares(shares);
				} else if (elem.toUpperCase().startsWith("BUY=")) {
					boolean buy = "TRUE".equals(dataStr.toUpperCase());
					order.setBuy(buy);
				} else if (elem.toUpperCase().startsWith("TYPE=")) {
					String type = dataStr;
					order.setType(type);
				} else if (elem.toUpperCase().startsWith("GTD=")) {
					try {
						Date gtd = df2.parse(dataStr);
						order.setGtd(gtd);
					} catch (ParseException e) {
						System.err.println("Error parsing gtd date in line: " + orderStr);
					}
				} else if (elem.toUpperCase().startsWith("STOP=")) {
					double stop = Double.parseDouble(dataStr);
					order.setStop(stop);
				} else if (elem.toUpperCase().startsWith("LMT=")) {
					double limit = Double.parseDouble(dataStr);
					order.setLimit(limit);
				} else if (elem.toUpperCase().startsWith("OCAGROUP=")) {
					String ocaGroup = dataStr;
					order.setOcaGroup(ocaGroup);
				} else if (elem.toUpperCase().startsWith("STRATEGY=")) {
					String strategy = dataStr;
					order.setStrategy(strategy);
				} else if (elem.toUpperCase().startsWith("POSITION=")) {
					int position = Integer.parseInt(dataStr);
					order.setPosition(position);
				} else if (elem.toUpperCase().startsWith("PARENTID=")) {
					int parentId = Integer.parseInt(dataStr);
					order.setParentId(parentId);
				} else if (elem.toUpperCase().startsWith("AVGFILLPRICE=")) {
					double avgFillPrice = Double.parseDouble(dataStr);
					order.setAvgFillPrice(avgFillPrice);
				} else if (elem.toUpperCase().startsWith("REMAINING=")) {
					int remaining = Integer.parseInt(dataStr);
					order.setRemaining(remaining);
				} else if (elem.toUpperCase().startsWith("FILLED=")) {
					int filled = Integer.parseInt(dataStr);
					order.setFilled(filled);
				} else if (elem.toUpperCase().startsWith("QTY=")) {
					int qty = Integer.parseInt(dataStr);
					order.setShares(qty);
				}
			}
		}
		
		return order;
	}
}
