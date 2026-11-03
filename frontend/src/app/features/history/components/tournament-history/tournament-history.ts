import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TournamentApiService } from '../../../tournament/services/tournament-api.service';
import { TournamentHistoryEntry } from '../../models/tournament-history.model';
import { DatePipe } from '@angular/common';
import { TournamentFormat } from '../../../tournament/models/tournament.model';


@Component({
  selector: 'app-tournament-history',
  standalone: true,
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  templateUrl: './tournament-history.html',
  styleUrl: './tournament-history.scss',
})
export class TournamentHistory implements OnInit {
  private readonly tournamentApi = inject(TournamentApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly tournaments = signal<TournamentHistoryEntry[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadFailed = signal(false);

  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);

  ngOnInit(): void {
    this.loadTournamentHistory();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);

    this.loadTournamentHistory();
  }

  protected retry(): void {
    this.loadTournamentHistory();
  }

  protected viewResults(tournamentId: number): void {
    this.router.navigate(['/tournaments', tournamentId, 'results']);
  }

  private loadTournamentHistory(): void {
    if (this.loading()) {
      return;
    }

    this.loading.set(true);
    this.loadFailed.set(false);

    this.tournamentApi
      .getTournamentHistory(this.pageIndex(), this.pageSize())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.tournaments.set(response.content);
          this.pageIndex.set(response.page);
          this.pageSize.set(response.size);
          this.totalElements.set(response.totalElements);
        },
        error: () => {
          this.tournaments.set([]);
          this.loadFailed.set(true);
        },
      });
  }
}