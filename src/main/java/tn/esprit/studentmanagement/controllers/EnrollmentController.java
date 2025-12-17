package tn.esprit.studentmanagement.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.studentmanagement.dto.EnrollmentDTO;
import tn.esprit.studentmanagement.entities.Enrollment;
import tn.esprit.studentmanagement.services.IEnrollment;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/Enrollment")
@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
public class EnrollmentController {
    IEnrollment enrollmentService;
    @GetMapping("/getAllEnrollment")
    public List<EnrollmentDTO> getAllEnrollmentDisplay() {
        return enrollmentService.getAllEnrollments()
                .stream()
                .map(enrollmentService::toDTO) // transformation vers DTO
                .collect(Collectors.toList());
    }

    @GetMapping("/getEnrollment/{id}")
    public EnrollmentDTO getEnrollment(@PathVariable Long id) {
        return enrollmentService.toDTO(enrollmentService.getEnrollmentById(id));
    }
    @PostMapping("/createEnrollment")
    public Enrollment createEnrollment(@RequestBody Enrollment enrollment) { return enrollmentService.saveEnrollment(enrollment); }

    @PutMapping("/updateEnrollment")
    /*public Enrollment updateEnrollment(@RequestBody Enrollment enrollment) {
        return enrollmentService.saveEnrollment(enrollment);
    }*/
    public EnrollmentDTO updateEnrollment(@RequestBody Enrollment enrollment) {
        Enrollment updated = enrollmentService.saveEnrollment(enrollment);
        return enrollmentService.toDTO(updated);
    }

    @DeleteMapping("/deleteEnrollment/{id}")
    public void deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id); }
}
