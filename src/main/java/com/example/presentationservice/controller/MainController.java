package com.example.presentationservice.controller;

import com.example.presentationservice.model.Answer;
import com.example.presentationservice.model.SearchTopicWrapper;
import com.example.presentationservice.model.Topic;
import com.example.presentationservice.model.User;
import com.example.presentationservice.model.UserRole;
import com.example.presentationservice.service.ArchivationService;
import com.example.presentationservice.service.PersistenceService;
import com.example.presentationservice.service.UserService;
import com.example.presentationservice.utils.PageUtil;
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.example.presentationservice.utils.Constants.LIMIT;
import static com.example.presentationservice.utils.DateUtils.getCurrentTimestamp;
import static com.example.presentationservice.utils.HashUtils.hashPassword;
import static java.util.stream.Collectors.toList;

@Controller
@Scope("session")
@SessionAttributes(value="sessionUser")
public class MainController {

    private final UserService userService;
    private final PersistenceService persistenceService;
    private final ArchivationService archivationService;

    @Autowired
    public MainController(UserService userService,
                          PersistenceService persistenceService,
                          ArchivationService archivationService) {
        this.userService = userService;
        this.persistenceService = persistenceService;
        this.archivationService = archivationService;
    }

    private User sessionUser = new User();
    private Future<byte[]> futureFile;
    private String currentTopicId;

    // *** Main page with topics
    @GetMapping(value = {"/", "/index", "/index/{page}"})
    public String getIndex(
            @ModelAttribute("topic") Topic topic,
            @PathVariable Optional<Integer> page,
            Model model) {

        Integer currentPage = page.orElse(0);
        long topicsCount = persistenceService.getTopicCount();
        currentPage = PageUtil.pageValidation(currentPage, topicsCount);

        model.addAttribute("topic", topic);
        model.addAttribute("page", currentPage);
        model.addAttribute("sessionUser", sessionUser);
        model.addAttribute("listTopic", persistenceService.getTopics(currentPage));
        model.addAttribute("pages", PageUtil.getPages(topicsCount));
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
            final BindingResult bindingResult) {

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
    @GetMapping(value = {"/answer/{topicId}", "/answer/{topicId}/{page}"})
    public String showAnswer(
            @PathVariable("topicId") String topicId,
            @PathVariable Optional<Integer> page,
            Model model) {


        Integer currentPage = page.orElse(0);
        List<Answer> answers = persistenceService.getTopicById(topicId).getAnswers();
        long answersCount = answers.size();
        currentPage = PageUtil.pageValidation(currentPage, answersCount);

        model.addAttribute("page", currentPage);
        model.addAttribute("listAnswer", answers.stream().skip(currentPage * LIMIT).limit(LIMIT).collect(toList()));
        model.addAttribute("pages", PageUtil.getPages(answersCount));
        model.addAttribute("answer", new Answer());
        model.addAttribute("topicId", topicId);
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
            @PathVariable("userId") String userId,
            @PathVariable("date") Long date) {

        persistenceService.deleteAnswerByUserAndDate(topicId, userId, date);
        return "redirect:/answer/{topicId}/";
    }

    @GetMapping("/search")
    public String setupForm(Model model) {
        model.addAttribute("search", new SearchTopicWrapper());
        return "search";
    }

    @PostMapping("/search")
    public String doSearch(SearchTopicWrapper search, Model model) {
        model.addAttribute("search", search);
        model.addAttribute("results", persistenceService.findTopicByText(search.getSearchString()));
        return "search";
    }

    @Scope("session")
    @GetMapping("/archivation")
    public String archivShow(Model model) {
        if(futureFile != null) {
            if(futureFile.isDone()) {
                model.addAttribute("name", "Ready for download");
            }
            else {
                model.addAttribute("name", "Your archive is being created now ...");
            }
        }
        else {
            model.addAttribute("name", "Click 'get archive' for download");
        }
        return "archivation";
    }

    @Scope("session")
    @PostMapping(value = "/archivation", params = { "archivation" })
    public String archivProcess(Model model) {
        model.addAttribute("name", "Click 'get archive' for download");
        try {
            futureFile = archivationService.getArchieve();
            if(futureFile.isDone()) {
                model.addAttribute("name", "Ready for download");
                model.addAttribute("file", futureFile);
            } else {
                model.addAttribute("name", "Your archive is being created now ...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "archivation";
    }

    @Scope("session")
    @GetMapping("/download")
    public void doDownloadFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ExecutionException, InterruptedException {
        ServletContext context = request.getServletContext();

        ByteArrayInputStream downloadFile = new ByteArrayInputStream(futureFile.get());
        response.setContentType("application/octet-stream");
        response.setContentLength(futureFile.get().length);
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"",
                "all_topics.zip");
        response.setHeader(headerKey, headerValue);
        OutputStream outStream = response.getOutputStream();

        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        // write bytes read from the input stream into the output stream
        while ((bytesRead = downloadFile.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        downloadFile.close();
        outStream.close();
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
        futureFile = null;
        return "redirect:/index";
    }
}
