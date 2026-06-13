package com.learn.appjava.controller;

import com.learn.appjava.model.User;
import com.learn.appjava.service.UserService2;
import com.learn.appjava.service.rabbitmq.UserEventRabbitProducer;
import com.learn.appjava.service.redis.UserService1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService1 userService;
//    private final UserService2 userService;

    private final UserEventRabbitProducer rabbitProducer;

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        return ResponseEntity.ok(userService.create(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/direct")
    public ResponseEntity<Void> sendDirect(@PathVariable Long id) {
        User user = userService.getById(id);
        rabbitProducer.sendDirect(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/fanout")
    public ResponseEntity<Void> sendFanout(@PathVariable Long id) {
        User user = userService.getById(id);
        rabbitProducer.sendFanout(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/topic")
    public ResponseEntity<Void> sendTopic(@PathVariable Long id) {
        User user = userService.getById(id);
        rabbitProducer.sendTopic(user);
        return ResponseEntity.ok().build();
    }
}