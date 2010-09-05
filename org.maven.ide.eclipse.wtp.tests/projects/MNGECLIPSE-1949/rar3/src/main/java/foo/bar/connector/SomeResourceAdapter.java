package foo.bar.connector;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

public class SomeResourceAdapter implements ResourceAdapter {

	public void endpointActivation(MessageEndpointFactory arg0,
			ActivationSpec arg1) throws ResourceException {
		// TODO Auto-generated method stub

	}

	public void endpointDeactivation(MessageEndpointFactory arg0,
			ActivationSpec arg1) {
		// TODO Auto-generated method stub

	}

	public XAResource[] getXAResources(ActivationSpec[] arg0)
			throws ResourceException {
		// TODO Auto-generated method stub
		return null;
	}

	public void start(BootstrapContext arg0)
			throws ResourceAdapterInternalException {
		System.out.println("Starting test adapter");

	}

	public void stop() {
		System.out.println("Stopping test adapter");

	}

}
