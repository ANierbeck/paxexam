package org.sebersole.paxexam;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.repositories;
import static org.ops4j.pax.exam.CoreOptions.repository;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Steve Ebersole
 */
@RunWith( PaxExam.class )
@ExamReactorStrategy( PerClass.class )
public class PaxExamTest {

	@Configuration
	public Option[] config() throws Exception {
		Properties paxExamEnvironment = loadPaxExamEnvironmentProperties();

		return options(
				karafDistributionConfiguration()
						.frameworkUrl( paxExamEnvironment.getProperty( "org.ops4j.pax.exam.container.karaf.distroUrl" ) )
						.karafVersion( paxExamEnvironment.getProperty( "org.ops4j.pax.exam.container.karaf.version" ) )
						.name( "Apache Karaf" )
						.unpackDirectory( new File( paxExamEnvironment.getProperty( "org.ops4j.pax.exam.container.karaf.unpackDir" ) ) )
						.useDeployFolder( false )
				, repositories(
						repository( "https://repository.jboss.org/nexus/content/groups/public-jboss/" )
								.id( "jboss-nexus" )
								.allowSnapshots()
				)
				, features( featureXmlUrl(), "hibernate-native" )
				, features( testingFeatureXmlUrl(), "hibernate-osgi-testing" )
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



	@Inject
	@SuppressWarnings("UnusedDeclaration")
	private BundleContext bundleContext;
	
	@Inject
	protected FeaturesService featuresService;

	@Inject
	BootFinished bootFinished;

	
	@Test
	public void testFeature() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService.getFeature("hibernate-native")));
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
