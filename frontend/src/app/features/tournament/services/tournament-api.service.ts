// features/tournament/data-access/tournament-api.service.ts
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NewTournamentRequest } from '../../setup/models/setup.model';
import { Tournament } from '../models/tournament.model';

@Injectable({ providedIn: 'root' })
export class TournamentApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tournaments';

  create(request: NewTournamentRequest): Observable<Tournament> {
    return this.http.post<Tournament>(this.baseUrl, request);
  }
}