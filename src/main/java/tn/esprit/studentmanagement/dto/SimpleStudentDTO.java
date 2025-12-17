package tn.esprit.studentmanagement.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleStudentDTO {
    private Long idStudent;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}