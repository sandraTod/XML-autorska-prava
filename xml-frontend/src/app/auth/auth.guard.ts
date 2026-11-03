import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    if (!this.auth.token) return this.router.createUrlTree(['/auth/prijava']);
    if (route.data['redirectCitizen'] && this.auth.role === 'GRADJANIN') {
      return this.router.createUrlTree(['/moji-zahtevi']);
    }
    if (route.data['redirectOfficial'] && this.auth.role === 'SLUZBENIK') {
      return this.router.createUrlTree(['/sluzbenik/zahtevi']);
    }
    const roles: string[] | undefined = route.data['roles'];
    return !roles || roles.includes(this.auth.role || '') ? true : this.router.createUrlTree(['/']);
  }
}
