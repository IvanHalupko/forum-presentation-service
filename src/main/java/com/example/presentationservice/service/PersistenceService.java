package com.example.presentationservice.service;

import com.example.presentationservice.model.Answer;
import com.example.presentationservice.model.Topic;
import com.example.presentationservice.model.TopicCountWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.presentationservice.utils.Constants.HTTP_PREFIX;

@Service
public class PersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceService.class);

    @Value("${services.data}")
    private String persistenceServiceName;

    @Autowired
    @LoadBalanced
    private RestTemplate restTemplate;

    public List<Topic> getTopics(Integer page) {
        ResponseEntity<List<Topic>> responseEntity = restTemplate.exchange(
                HTTP_PREFIX + persistenceServiceName + "/topics/" + page,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Topic>>() {
                });

        return responseEntity.getBody();
    }

    public List<Topic> getAllTopicsForArchivation() {
        ResponseEntity<List<Topic>> responseEntity = restTemplate.exchange(
                HTTP_PREFIX + persistenceServiceName + "/topics",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Topic>>() {
                });

        return responseEntity.getBody();
    }

    public long getTopicCount() {
        TopicCountWrapper count = restTemplate
                .getForObject(HTTP_PREFIX + persistenceServiceName + "/topic-count", TopicCountWrapper.class);

        // TODO: logger
        return count.getTopicCount() == null ? 0 : count.getTopicCount();
    }

    public Topic getTopicById(String id) {
        Topic topic = restTemplate.getForObject(HTTP_PREFIX + persistenceServiceName + "/topic/" + id, Topic.class);
        List<Answer> answerList = topic != null ? topic.getAnswers() : null;
        if(answerList != null) {
            List<Answer> answers = answerList.stream()
                    .sorted(Comparator.comparing(Answer::getDateAnswer).reversed())
                    .collect(Collectors.toList());
            topic.setAnswers(answers);
        } else {
            topic.setAnswers(Collections.emptyList());
        }
        return topic;
    }

    public void saveTopic(Topic topic) {
        String topicId = restTemplate
                .postForObject(HTTP_PREFIX + persistenceServiceName + "/topic", topic, String.class);

        //TODO: logger
    }

    public void deleteTopicById(String topicId) {
        restTemplate.delete(HTTP_PREFIX + persistenceServiceName + "/topic/" + topicId);
    }

    public void saveAnswer(Answer answer, String topicId) {
        String topicIdAfterUpdate = restTemplate
                .postForObject(HTTP_PREFIX + persistenceServiceName + "/answer/" + topicId, answer, String.class);

        //TODO: logger
    }

    public void deleteAnswerByUserAndDate(String topicId, String userId, Long date) {
        restTemplate.delete(HTTP_PREFIX + persistenceServiceName + "/answer/" + topicId + "/" + userId + "/" + date);
    }

    public Topic findTopicByText(String text) {
        Topic foundTopic = restTemplate
                .getForObject(HTTP_PREFIX + persistenceServiceName + "/search/" + text, Topic.class);

        // TODO: logger
        return foundTopic;
    }

}
