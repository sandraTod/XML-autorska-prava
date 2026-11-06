import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './auth/auth.guard';
import { PocetnaComponent } from './pocetna/pocetna.component';
import { MojiZahteviComponent } from './a1/moji-zahtevi/moji-zahtevi.component';
import { PregledZahtevaComponent } from './a1/pregled-zahteva/pregled-zahteva.component';
import { SviZahteviComponent } from './a1/svi-zahtevi/svi-zahtevi.component';
import { NoviZahtevComponent } from './a1/novi-zahtev/novi-zahtev.component';
import { PretragaComponent } from './a1/pretraga/pretraga.component';

const routes: Routes = [
  { path: 'pretraga', component: PretragaComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN', 'SLUZBENIK'] } },
  { path: 'sluzbenik/zahtevi', component: SviZahteviComponent, canActivate: [AuthGuard], data: { roles: ['SLUZBENIK'] } },
  {
    path: 'sluzbenik/zahtevi/:documentId', component: PregledZahtevaComponent, canActivate: [AuthGuard],
    data: { roles: ['SLUZBENIK'], listRoute: '/sluzbenik/zahtevi', listTitle: 'Svi zahtevi' }
  },
  { path: 'novi-zahtev', component: NoviZahtevComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN'] } },
  { path: 'moji-zahtevi', component: MojiZahteviComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN'] } },
  { path: 'moji-zahtevi/:documentId', component: PregledZahtevaComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN'] } },
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(module => module.AuthModule) },
  { path: '', component: PocetnaComponent, pathMatch: 'full', canActivate: [AuthGuard], data: { roles: ['GRADJANIN', 'SLUZBENIK'], redirectCitizen: true, redirectOfficial: true } },
  { path: '**', redirectTo: '' }
];

@NgModule({ imports: [RouterModule.forRoot(routes)], exports: [RouterModule] })
export class AppRoutingModule {}
