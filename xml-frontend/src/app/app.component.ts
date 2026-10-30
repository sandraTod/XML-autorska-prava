import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth/auth.service';

@Component({ selector: 'app-root', templateUrl: './app.component.html', styleUrls: ['./app.component.css'] })
export class AppComponent {
  title = 'Autorska prava';
  constructor(public auth: AuthService, private router: Router) {}
  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/auth/prijava']);
  }
}
