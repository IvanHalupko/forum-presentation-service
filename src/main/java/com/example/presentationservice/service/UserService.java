package com.example.presentationservice.service;

import com.example.presentationservice.model.LoginCheckWrapper;
import com.example.presentationservice.model.User;
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

import java.util.List;

import static com.example.presentationservice.utils.Constants.HTTP_PREFIX;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Value("${services.user}")
    private String userServiceName;

    @Autowired
    @LoadBalanced
    private RestTemplate restTemplate;

    public List<User> getUsers() {
        ResponseEntity<List<User>> responseEntity = restTemplate.exchange(
                HTTP_PREFIX + userServiceName + "/user",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<User>>() {
                });

        return responseEntity.getBody();
    }

    public User getUserById(String id) {
        return restTemplate.getForObject(HTTP_PREFIX + userServiceName + "/user/" + id, User.class);
    }

    public boolean isLoginExists(String login) {
        LoginCheckWrapper isLoginExists = restTemplate.getForObject(HTTP_PREFIX + userServiceName + "/check-login/" + login, LoginCheckWrapper.class);

        return isLoginExists.getIsLoginExists();
    }

    public User authenticationUser(User user) {
        return restTemplate.postForObject(HTTP_PREFIX + userServiceName + "/auth-user", user, User.class);
    }

    public void saveUser(User user) {
        String userId = restTemplate.postForObject(HTTP_PREFIX + userServiceName + "/user", user, String.class);

        //TODO: logger
    }

    public String blockUser(String userId) {
        String blockedUserId = restTemplate.getForObject(HTTP_PREFIX + userServiceName + "/block-user/" + userId, String.class);

        return blockedUserId;
    }
}
