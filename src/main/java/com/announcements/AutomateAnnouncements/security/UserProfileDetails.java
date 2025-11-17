package com.announcements.AutomateAnnouncements.security;

import com.announcements.AutomateAnnouncements.entities.UserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserProfileDetails implements UserDetails {

    private final UserProfile userProfile;

    public UserProfileDetails(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userProfile.getRole()));
    }

    @Override
    public String getPassword() {
        return userProfile.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return userProfile.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
