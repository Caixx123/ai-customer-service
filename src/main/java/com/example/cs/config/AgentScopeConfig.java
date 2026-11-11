package com.example.cs.config;

import com.example.cs.agent.CustomerServicePrompt;
import com.example.cs.agent.tools.ExpertDelegateTools;
import com.example.cs.agent.tools.HandoffTools;
import com.example.cs.agent.tools.KnowledgeTools;
import com.example.cs.agent.tools.OrderTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class AgentScopeConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentScopeConfig.class);

    @Bean
    public OpenAIChatModel openAIChatModel(AppProperties properties) {
        AppProperties.Ai ai = properties.ai();
        if (ai.apiKey() == null || ai.apiKey().isBlank()) {
            log.warn("AI_API_KEY is not configured. Chat requests will fail until a valid key is provided.");
        }
        String apiKey = (ai.apiKey() == null || ai.apiKey().isBlank()) ? "missing-api-key" : ai.apiKey();
        log.info("Configuring OpenAI-compatible chat model: baseUrl={}, model={}", ai.baseUrl(), ai.model());
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(ai.baseUrl())
                .modelName(ai.model())
                .stream(true)
                .build();
    }

    @Bean
    public Toolkit orderToolkit(OrderTools orderTools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(orderTools);
        return toolkit;
    }

    @Bean
    public Toolkit policyToolkit(KnowledgeTools knowledgeTools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(knowledgeTools);
        return toolkit;
    }

    @Bean
    public ReActAgent orderExpertAgent(OpenAIChatModel model, @Qualifier("orderToolkit") Toolkit orderToolkit,
                                       AppProperties properties) {
        return ReActAgent.builder()
                .name("order-expert")
                .sysPrompt(CustomerServicePrompt.ORDER_EXPERT)
                .model(model)
                .toolkit(orderToolkit)
                .maxIters(properties.agent().maxIters())
                .build();
    }

    @Bean
    public ReActAgent policyExpertAgent(OpenAIChatModel model, @Qualifier("policyToolkit") Toolkit policyToolkit,
                                        AppProperties properties) {
        return ReActAgent.builder()
                .name("policy-expert")
                .sysPrompt(CustomerServicePrompt.POLICY_EXPERT)
                .model(model)
                .toolkit(policyToolkit)
                .maxIters(properties.agent().maxIters())
                .build();
    }

    @Bean
    public ExpertDelegateTools expertDelegateTools(
            @Qualifier("orderExpertAgent") ReActAgent orderExpert,
            @Qualifier("policyExpertAgent") ReActAgent policyExpert) {
        return new ExpertDelegateTools(orderExpert, policyExpert);
    }

    @Bean
    public Toolkit supervisorToolkit(ExpertDelegateTools expertDelegateTools, HandoffTools handoffTools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(expertDelegateTools);
        toolkit.registerTool(handoffTools);
        return toolkit;
    }

    @Bean
    public ReActAgent supervisorAgent(OpenAIChatModel model,
                                      @Qualifier("supervisorToolkit") Toolkit supervisorToolkit,
                                      AppProperties properties) throws Exception {
        Path stateDirectory = Path.of(properties.agent().stateDir()).toAbsolutePath().normalize();
        Files.createDirectories(stateDirectory);
        log.info("Supervisor state directory: {}", stateDirectory);

        return ReActAgent.builder()
                .name("customer-service-supervisor")
                .sysPrompt(CustomerServicePrompt.SUPERVISOR)
                .model(model)
                .toolkit(supervisorToolkit)
                .maxIters(properties.agent().maxIters())
                .stateStore(new JsonFileAgentStateStore(stateDirectory))
                .build();
    }
}
