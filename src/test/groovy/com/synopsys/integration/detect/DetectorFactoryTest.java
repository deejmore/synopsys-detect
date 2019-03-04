package com.synopsys.integration.detect;

import javax.xml.parsers.DocumentBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import com.synopsys.integration.detect.configuration.ConnectionManager;
import com.synopsys.integration.detect.configuration.DetectConfiguration;
import com.synopsys.integration.detect.configuration.DetectPropertyMap;
import com.synopsys.integration.detect.configuration.DetectPropertySource;
import com.synopsys.integration.detect.property.SpringPropertySource;
import com.synopsys.integration.detect.util.executable.CacheableExecutableFinder;
import com.synopsys.integration.detect.util.executable.ExecutableFinder;
import com.synopsys.integration.detect.util.executable.ExecutableRunner;
import com.synopsys.integration.detect.workflow.ArtifactResolver;
import com.synopsys.integration.detect.workflow.file.AirGapManager;
import com.synopsys.integration.detect.workflow.file.DetectFileFinder;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;

import freemarker.template.Configuration;

public class DetectorFactoryTest {

    AnnotationConfigApplicationContext runContext;

    @Before
    public void createSpringContext() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        DetectConfiguration mockConfig = new DetectConfiguration(new DetectPropertySource(new SpringPropertySource(environment)), new DetectPropertyMap());

        runContext = new AnnotationConfigApplicationContext();
        runContext.setDisplayName("Detect Run Test");
//        runContext.register(DetectorBeanConfiguration.class); TODO Fix
        runContext.getBeanFactory().registerSingleton(Gson.class.getSimpleName(), new Gson());
        runContext.getBeanFactory().registerSingleton(JsonParser.class.getSimpleName(), new JsonParser());
        registerMock(runContext, Configuration.class);
        registerMock(runContext, DocumentBuilder.class);
        registerMock(runContext, ExecutableRunner.class);
        registerMock(runContext, AirGapManager.class);
        registerMock(runContext, ExecutableFinder.class);
        registerMock(runContext, ExternalIdFactory.class);
        registerMock(runContext, DetectFileFinder.class);
        registerMock(runContext, DirectoryManager.class);
        registerMock(runContext, DetectConfiguration.class);
        registerMock(runContext, ConnectionManager.class);
        registerMock(runContext, CacheableExecutableFinder.class);
        registerMock(runContext, ArtifactResolver.class);
        registerMock(runContext, DetectInfo.class);

        runContext.refresh();
    }

    private <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> bean) {
        String name = bean.getSimpleName();
        context.getBeanFactory().registerSingleton(name, Mockito.mock(bean));
    }

    //@Test
    //TODO: add back in when using it
//    public void testNewBomToolsCreatedEveryTime() {
//        DetectorFactory detectorFactory = runContext.getBean(DetectorFactory.class);
//        DetectorSearchProvider provider = new DetectorSearchProvider(detectorFactory);
//
//        DetectorEnvironment mockEnv = Mockito.mock(DetectorEnvironment.class);
//
//        DetectorRuleSet ruleSet1 = provider.createBomToolSearchRuleSet(mockEnv);
//        DetectorRuleSet ruleSet2 = provider.createBomToolSearchRuleSet(mockEnv);
//
//        Detector detector1 = ruleSet1.getOrderedDetectorRules().get(0).getDetector();
//        Detector detector2 = ruleSet2.getOrderedDetectorRules().get(0).getDetector();
//
//        //Sanity check they are the same class
//        Assert.assertTrue(detector1.getClass().isInstance(detector2));
//        //And check they are not the same instance
//        Assert.assertFalse(detector1 == detector2);
//    }

}
