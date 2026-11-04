import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../auth.service';
import { PrijavaComponent } from './prijava.component';

describe('Login navigation', () => {
  it('sends officials to all requests and preserves citizen navigation', () => {
    for (const role of ['GRADJANIN', 'SLUZBENIK'] as const) {
      const auth = jasmine.createSpyObj<AuthService>('AuthService', ['login'], { role });
      auth.login.and.returnValue(of(undefined));
      const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
      router.navigate.and.resolveTo(true);
      const component = new PrijavaComponent(new FormBuilder(), auth, router);
      component.form.setValue({ email: 'primer@example.org', lozinka: ' pass word ' });
      component.submit();
      expect(auth.login).toHaveBeenCalledWith('primer@example.org', ' pass word ');
      expect(router.navigate).toHaveBeenCalledWith([role === 'GRADJANIN' ? '/moji-zahtevi' : '/sluzbenik/zahtevi']);
    }
  });
});
