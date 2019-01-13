package com.example.presentationservice.controller;

import com.example.presentationservice.model.Answer;
import com.example.presentationservice.model.SearchTopicWrapper;
import com.example.presentationservice.model.Topic;
import com.example.presentationservice.model.User;
import com.example.presentationservice.model.UserRole;
import com.example.presentationservice.service.PersistenceService;
import com.example.presentationservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import java.util.List;

import static com.example.presentationservice.utils.DateUtils.getCurrentTimestamp;
import static com.example.presentationservice.utils.HashUtils.hashPassword;

@Controller
@Scope("session")
@SessionAttributes(value="sessionUser")
public class MainController {

    private final UserService userService;

    private final PersistenceService persistenceService;

    @Autowired
    public MainController(UserService userService, PersistenceService persistenceService) {
        this.userService = userService;
        this.persistenceService = persistenceService;
    }

    private User sessionUser = new User();

    private String currentTopicId;

    // *** Main page with topics
    @GetMapping(value = {"/", "/index"})
    public String getIndex(@ModelAttribute("topic") Topic topic, Model model) {
        //new Topic()
//        resetCurrentPage();
        model.addAttribute("topic", topic);
//        model.addAttribute("page", String.valueOf(currentPage));
//        model.addAttribute("tPage", String.valueOf(topPage));
        model.addAttribute("sessionUser", sessionUser);
        model.addAttribute("listTopic", persistenceService.getTopics());
//        model.addAttribute("topAll", topicService.getAll());
//        model.addAttribute("usNm", topic.getUserName());
        return "/index";
    }

    // *** Remove topic button processing
    @GetMapping("/index/remove/{topicId}")
    public String getTopRemove(
            @ModelAttribute("topic") Topic topic,
            Model model,
            @PathVariable("topicId") String id) {

        model.addAttribute("topic", topic);
        model.addAttribute("sessionUser", sessionUser);
        persistenceService.deleteTopicById(id);
        return "redirect:/index";
    }

    // *** Get users page
    @GetMapping("/users")
    public String getUsers(Model model) {
        List<User> users = userService.getUsers();
        model.addAttribute("users", users);

        return "users";
    }

    // *** User information page
    @GetMapping("/user-info")
    public String userInfo(@ModelAttribute("sessionUser") User user, Model model) {

        model.addAttribute("sessionUser", sessionUser);
        return "userInfo";
    }

    // *** Registration new user page
    @GetMapping("/registration")
    public String registrationForm(Model model) {
        model.addAttribute("sessionUser", User.builder().build());
        return "registration";
    }

    // *** Registration new user processing
    @PostMapping(value = "/registration", params = { "addUser" })
    public String saveUser(
            @ModelAttribute("sessionUser") User user,
            Model model,
            final BindingResult bindingResult) throws Exception {

        if (bindingResult.hasErrors()) {
            return "registration";
        }
        user.setPassword(hashPassword(user.getPassword()));
        user.setIsUserBlock(false);
        user.setUserRole(UserRole.USER);
        model.addAttribute("sessionUser", user);
        if(!userService.isLoginExists(user.getLogin())) {
            userService.saveUser(user);
        } else {
            model.addAttribute("loginExists", "This login already exists");
            return "registration";
        }
        return "redirect:/index";
    }

    // *** Block user by admin button processing
    @GetMapping("/user/block/{userId}")
    public String getChangeUserStatus(@PathVariable("userId") String userId) {
        userService.blockUser(userId);
        return "redirect:/users";
    }

    // *** Add new topic processing
    @PostMapping(value = "/topic", params = { "addTopic" })
    public String addNewTopic(@ModelAttribute("topic") Topic topic,
                              Model model, final BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "topic";
        }
        model.addAttribute("topic", topic);
        if(sessionUser.getId() != null) {
            topic.setUserId(sessionUser.getId());
        } else {
            return "redirect:/login";
        }
        topic.setTopicDate(getCurrentTimestamp());
        topic.setUserName(sessionUser.getUserName());
        persistenceService.saveTopic(topic);
        return "redirect:/index";
    }

    // *** Get answers page
    @GetMapping("/answer/{topicId}")
    public String showAnswer(@PathVariable("topicId") String topicId, Model model) {
//        resetTopPage();
//        currentPage = page;
//        model.addAttribute("page", String.valueOf(currentPage));
        model.addAttribute("listAnswer", persistenceService.getTopicById(topicId).getAnswers());
//        model.addAttribute("answAll", answerService.getForId(id));
        model.addAttribute("answer", new Answer());
        currentTopicId = topicId;
        return "answer";
    }

    // *** Add new answer processing
    @PostMapping("/answer")
    public String addAnswer(
            @ModelAttribute("answer") Answer answer,
            Model model,
            final BindingResult bindingResult,
            Topic topic) {
        if (bindingResult.hasErrors()) {
            return "redirect:/index";
        }
        model.addAttribute("answer", answer);
        model.addAttribute("topic", topic);
        answer.setDateAnswer(getCurrentTimestamp());
        if(sessionUser.getId() == null) {
            return "redirect:/login";
        } else {
            answer.setUserId(sessionUser.getId());
        }
        persistenceService.saveAnswer(answer, currentTopicId);
        return "redirect:/index";
    }

    // *** Remove topic's answer button processing
    @GetMapping("/answer/{topicId}/remove/{userId}/{date}")
    public String getAnswerRemove(
            @PathVariable("topicId") String topicId,
//            @PathVariable("page") int page,
            @PathVariable("userId") String userId,
            @PathVariable("date") Long date) {

        persistenceService.deleteAnswerByUserAndDate(topicId, userId, date);
        return "redirect:/answer/{topicId}/";
    }

    @GetMapping("/search")
    public String setupForm(Model model) {
//        resetTopPage();
//        resetCurrentPage();
        model.addAttribute("search", new SearchTopicWrapper());
        return "search";
    }

    @PostMapping("/search")
    public String doSearch(SearchTopicWrapper search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("results", persistenceService.findTopicByText(search.getSearchString()));
        return "search";
    }

    // *** Login user page
    @GetMapping("/login")
    public String loginForm(@ModelAttribute("sessionUser") User user, Model model) {
        model.addAttribute("sessionUser", new User());
        model.addAttribute("login", user.getLogin());
        model.addAttribute("password", user.getPassword());
        return "login";
    }

    // *** Login user processing
    @PostMapping("/login")
    public String login(@ModelAttribute("sessionUser") User user, Model model) {
        model.addAttribute("sessionUser", user);
        user = userService.authenticationUser(
                User.builder()
                        .login(user.getLogin())
                        .password(hashPassword(user.getPassword()))
                        .build());
        if(user != null) {
            if(!user.getIsUserBlock()) {
                sessionUser = user;
                return "redirect:/index";
            } else {
                model.addAttribute("userWasBlocked", "You was blocked by administrator!");
                return "login";
            }

        }
        return "login";
    }

    // *** Logout user
    @GetMapping("/logout")
    public String logout(@ModelAttribute("sessionUser") User user, SessionStatus session) {
        session.setComplete();
        sessionUser = new User();
//        archivationService.reset();
//        tl = null;
        return "redirect:/index";
    }
}
