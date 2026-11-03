import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import {
  Subject,
  finalize
} from 'rxjs';

import { LeaderboardEntry } from '../../models/leaderboard.model';
import { LeaderboardApiService } from '../../services/leaderboard-api.service';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
  ],
  templateUrl: './leaderboard.html',
  styleUrl: './leaderboard.scss',
})
export class Leaderboard implements OnInit {
  private readonly leaderboardApi = inject(LeaderboardApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly displayedColumns = [
    'rank',
    'player',
    'tournamentWins',
    'tournamentsPlayed',
    'winRate',
  ];

  protected readonly entries = signal<LeaderboardEntry[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadFailed = signal(false);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);

  constructor() {}

  ngOnInit(): void {
    this.loadLeaderboard();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);

    this.loadLeaderboard();
  }

  protected retry(): void {
    this.loadLeaderboard();
  }

  private loadLeaderboard(): void {
    this.loading.set(true);
    this.loadFailed.set(false);

    this.leaderboardApi.getLeaderboard(this.pageIndex(), this.pageSize())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.entries.set(response.content);
          this.pageIndex.set(response.page);
          this.pageSize.set(response.size);
          this.totalElements.set(response.totalElements);
        },
        error: () => {
          this.entries.set([]);
          this.loadFailed.set(true);
        },
      });
  }
}