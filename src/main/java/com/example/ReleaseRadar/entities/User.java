package com.example.ReleaseRadar.entities;



import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {   //ROLE KULLANILMADI,PHONE NUMBER EKLENDİ

    @Id
    private Long id;
    @NotBlank(message = "İsim boş olamaz")
    @Size(max = 60, message = "İsim en fazla 60 karakter olabilir")
    @Column(name = "first_name", length = 60)
    private String firstName;
    @NotBlank(message = "Soyisim boş olamaz")
    @Size(max = 60, message = "Soyisim en fazla 60 karakter olabilir")
    @Column(name = "last_name", length = 60)
    private String lastName;
    @Email(message = "Geçerli bir email giriniz")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Telefon numarası sadece rakam içermeli ve 10-15 haneli olmalı")
    @NotBlank(message = "Phone Number is required")  //PHONE NUMBER EKLENDİ
    @Column(name = "phone_number")
    private String phoneNumber;
    private String department;
    private Boolean isActive;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;




    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
