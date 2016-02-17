package activemq.cli.command;

public enum DeliveryMode {

	PERSISTENT(javax.jms.DeliveryMode.PERSISTENT),
	NON_PERSISTENT(javax.jms.DeliveryMode.NON_PERSISTENT);

	private final int jmsDeliveryMode;

    DeliveryMode(int jmsDeliveryMode) {
    	this.jmsDeliveryMode = jmsDeliveryMode;
    }

    public int getJMSDeliveryMode() {
    	return jmsDeliveryMode;
    }
}
