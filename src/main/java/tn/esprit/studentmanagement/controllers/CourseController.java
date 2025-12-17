package tn.esprit.studentmanagement.controllers;

import tn.esprit.studentmanagement.dto.SimpleCourseDTO;
import tn.esprit.studentmanagement.entities.Course;
import tn.esprit.studentmanagement.services.ICourseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
@RestController
@RequestMapping("/courses")
@CrossOrigin(origins = "http://localhost:4200")
public class CourseController {

    private final ICourseService courseService;

    public CourseController(ICourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/getAllCourses")
    /*public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }*/
    public List<SimpleCourseDTO> getSimpleCourses() {
        return courseService.getAllSimpleCourseDTOs();
    }

    @GetMapping("/getCourse/{id}")
    public Optional<Course> getCourse(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @PostMapping("/createCourse")
    public Course createCourse(@RequestBody Course course) {
        return courseService.addCourse(course);
    }

    @PutMapping("/updateCourse{id}")
    public Course updateCourse(@PathVariable Long id, @RequestBody Course course) {
        course.setIdCourse(id); // Assure-toi que l'id est correct
        return courseService.updateCourse(course);
    }

    @DeleteMapping("/deleteCourse{id}")
    public void deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }
}