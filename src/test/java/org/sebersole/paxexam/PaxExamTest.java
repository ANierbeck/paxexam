package org.sebersole.paxexam;

import java.io.File;
import javax.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.repositories;
import static org.ops4j.pax.exam.CoreOptions.repository;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;

/**
 * @author Steve Ebersole
 */
@RunWith( PaxExam.class )
@ExamReactorStrategy( PerClass.class )
public class PaxExamTest {

	@Configuration
	public Option[] config() throws Exception {
		return options(
				debugConfiguration( "5005", true ),
				karafDistributionConfiguration()
						.frameworkUrl(
								maven().groupId( "org.apache.karaf" )
										.artifactId( "apache-karaf" )
										.type( "tar.gz" )
										.version( "3.0.3" )
						)
						.karafVersion( "3.0.3" )
						.name( "Apache Karaf" )
						.unpackDirectory( new File( "target/karaf" ) )
						.useDeployFolder( false ),
				repositories(
						repository( "https://repository.jboss.org/nexus/content/groups/public-jboss/" )
								.id( "jboss-nexus" )
								.allowSnapshots()
				),
				features( featureXmlUrl(), "hibernate-native", "hibernate-jpa" ),
				features( testingFeatureXmlUrl(), "hibernate-osgi-testing" )
				//, mavenBundle().groupId( "com.h2database" ).artifactId( "h2" ).version( "1.3.170" ).start()
		);
	}

	private static String featureXmlUrl() {
		return PaxExamTest.class.getClassLoader().getResource(
				"org/sebersole/paxexam/poc/karaf-feature/hibernate-osgi-5.0.0-SNAPSHOT-karaf.xml"
		).toExternalForm();
	}

	private String testingFeatureXmlUrl() {
		return PaxExamTest.class.getClassLoader().getResource(
				"org/sebersole/paxexam/poc/testing-bundles.xml"
		).toExternalForm();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Prepare the PaxExam probe (the bundle to deploy)

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		System.out.println( "Configuring probe..." );

//		// attempt to override PaxExam's default of dynamically importing everything
//		probe.setHeader( Constants.DYNAMICIMPORT_PACKAGE, "" );
		// and use defined imports instead
		probe.setHeader(
				Constants.IMPORT_PACKAGE,
				"javassist.util.proxy" +
						",javax.persistence" +
						",javax.persistence.spi" +
						",org.h2" +
						",org.osgi.framework" +
						",org.hibernate" +
						",org.hibernate.boot.model" +
						",org.hibernate.boot.registry.selector" +
						",org.hibernate.boot.registry.selector.spi" +
						",org.hibernate.cfg" +
						",org.hibernate.engine.spi" +
						",org.hibernate.integrator.spi" +
						",org.hibernate.proxy" +
						",org.hibernate.service" +
						",org.hibernate.service.spi"
//						+ ",org.ops4j.pax.exam.options"
//						+ ",org.ops4j.pax.exam"
		);
//		probe.setHeader( Constants.BUNDLE_ACTIVATOR, "org.hibernate.osgi.test.client.OsgiTestActivator" );
		return probe;
	}


	@Inject
	@SuppressWarnings("UnusedDeclaration")
	private BundleContext bundleContext;

	@Test
	public void test() throws Exception {
		System.out.println( "Injected BundleContext shows the following bundles available:" );
		for ( Bundle bundle : bundleContext.getBundles() ) {
			System.out.println( "    > " + bundle.getSymbolicName() + ":" + bundle.getVersion() );
		}
	}

	@Test
	public void testNative() throws Exception {
		final ServiceReference sr = bundleContext.getServiceReference( SessionFactory.class.getName() );
		final SessionFactory sf = (SessionFactory) bundleContext.getService( sr );

		Session s = sf.openSession();
		s.getTransaction().begin();
		s.persist( new DataPoint( "Brett" ) );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		DataPoint dp = (DataPoint) s.get( DataPoint.class, 1 );
		assertNotNull( dp );
		assertEquals( "Brett", dp.getName() );
		s.getTransaction().commit();
		s.close();

		dp.setName( "Brett2" );

		s = sf.openSession();
		s.getTransaction().begin();
		s.update( dp );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		dp = (DataPoint) s.get( DataPoint.class, 1 );
		assertNotNull( dp );
		assertEquals( "Brett2", dp.getName() );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from DataPoint" ).executeUpdate();
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		dp = (DataPoint) s.get( DataPoint.class, 1 );
		assertNull( dp );
		s.getTransaction().commit();
		s.close();
	}
}
