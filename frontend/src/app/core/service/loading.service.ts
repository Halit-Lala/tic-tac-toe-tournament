import { computed, Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pending = signal(0);

  readonly loading = computed(() => this.pending() > 0);

  start(): void {
    this.pending.update((count) => count + 1);
  }

  // stop(): void {
  //   this.pending.update((count) => Math.max(0, count - 1));
  // }

  stop(): void {
    setTimeout(
      () => this.pending.update((count) => Math.max(0, count - 1)),
      500,
    );
  }
}