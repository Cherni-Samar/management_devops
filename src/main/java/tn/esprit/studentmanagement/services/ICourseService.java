package tn.esprit.studentmanagement.services;

import tn.esprit.studentmanagement.dto.SimpleCourseDTO;
import tn.esprit.studentmanagement.entities.Course;
import java.util.List;
import java.util.Optional;

public interface ICourseService {
    Course addCourse(Course course);
    Course updateCourse(Course course);
    void deleteCourse(Long id);
    Optional<Course> getCourseById(Long id);
    List<Course> getAllCourses();
    public List<SimpleCourseDTO> getAllSimpleCourseDTOs();
}