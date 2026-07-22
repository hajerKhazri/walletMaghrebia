import { Component, OnInit, inject } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  
  isAuthenticated = false;
  isAuthPage = false;

  ngOnInit(): void {
    this.authService.authState$.subscribe(state => {
      this.isAuthenticated = state.isAuthenticated;
    });

    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.isAuthPage = event.url.includes('/auth');
      }
    });
  }
}