package org.keycloak.quickstart;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.helper.TestsHelper;
import org.keycloak.quickstart.appjee.Controller;
import org.keycloak.quickstart.page.IndexPage;
import org.keycloak.quickstart.page.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.quickstart.page.IndexPage.MESSAGE_PUBLIC;
import static org.keycloak.quickstart.page.IndexPage.UNAUTHORIZED;
import static org.keycloak.quickstart.utils.WaitUtils.waitForPageToLoad;
import static org.keycloak.quickstart.utils.WaitUtils.waitTextToBePresent;

/**
 * Created by abstractj on 3/13/17.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ArquillianTest {

    private static final String WEBAPP_SRC = "src/main/webapp";

    @Page
    private IndexPage indexPage;

    @Page
    private LoginPage loginPage;

    @Deployment(name= "service-jee-jaxrs", order = 1, testable = false)
    public static Archive<?> createTestArchive1() throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File("../service-jee-jaxrs/target/service.war"));
    }

    @Deployment(name= "app-jsp", order = 2, testable = false)
    public static Archive<?> createTestArchive2() throws IOException {
        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                    .importRuntimeDependencies().resolve().withTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class,  "app-jsp.war")
                .addPackages(true, Filters.exclude(".*Test.*"), Controller.class.getPackage())
                .addAsLibraries(files)
                .addAsWebResource(new File(WEBAPP_SRC, "index.jsp"))
                .addAsWebResource(new File(WEBAPP_SRC, "protected.jsp"))
                .addAsWebResource(new File(WEBAPP_SRC, "styles.css"))
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new File("config", "keycloak.json"))
                .setWebXML(new File("src/main/webapp", "WEB-INF/web.xml"));
    }

    @Drone
    private WebDriver webDriver;

    @ArquillianResource
    @OperateOnDeployment("app-jsp")
    private URL contextRoot;

    @Before
    public void setup() {
        webDriver.navigate().to(contextRoot);
    }

    @Test
    public void testSecuredResource() throws InterruptedException {
        try {
            indexPage.clickSecured();
            assertTrue(waitTextToBePresent(webDriver, By.className("error"), UNAUTHORIZED));
        } catch (Exception e) {
            fail("Should display an error message");
        }
    }

    @Test
    public void testAdminResource() {
        try {
            indexPage.clickAdmin();
            assertTrue(waitTextToBePresent(webDriver, By.className("error"), UNAUTHORIZED));
        } catch (Exception e) {
            fail("Should display an error message");
        }
    }

    @Test
    public void testPublicResource() {
        try {
            indexPage.clickPublic();
            waitTextToBePresent(webDriver, By.id("message"), MESSAGE_PUBLIC);
        } catch (Exception e) {
            fail("Should display an error message");
        }
    }

    @Test
    public void testAdminWithAuthAndRole() throws MalformedURLException, InterruptedException {
        try {
            indexPage.clickLogin();
            loginPage.login("admin", "admin");
            indexPage.clickAdmin();
            assertTrue(waitTextToBePresent(webDriver, By.id("message"), "Message: admin"));
            indexPage.clickLogout();
        } catch (Exception e) {
            fail("Should display logged in user");
        }
    }

    @Test
    public void testUserWithAuthAndRole() throws MalformedURLException, InterruptedException {
        try {
            indexPage.clickLogin();
            loginPage.login("user", "user");
            indexPage.clickSecured();
            assertTrue(waitTextToBePresent(webDriver, By.id("message"), "Message: secured"));
            indexPage.clickLogout();
        } catch (Exception e) {
            fail("Should display logged in user");
        }
    }
}
