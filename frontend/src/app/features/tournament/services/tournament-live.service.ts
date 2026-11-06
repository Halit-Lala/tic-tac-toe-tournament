import { Injectable } from "@angular/core";
import { Tournament } from "../models/tournament.model";
import { defer, finalize, map, Observable } from "rxjs";
import { RxStomp } from '@stomp/rx-stomp';

@Injectable({ providedIn: 'root' })
export class TournamentLiveService {
  private readonly rxStomp = new RxStomp();
  private watchers = 0;

  readonly connected$ = this.rxStomp.connected$;

  constructor() {
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    this.rxStomp.configure({ brokerURL: `${scheme}://${location.host}/ws`, reconnectDelay: 3000 });
  }

  watch(id: number): Observable<Tournament> {
    return defer(() => {
      if (this.watchers++ === 0) {
        this.rxStomp.activate();
      }
      return this.rxStomp.watch(`/topic/tournaments/${id}`).pipe(
        map((message) => JSON.parse(message.body) as Tournament),
        finalize(() => {
          if (--this.watchers === 0) {
            void this.rxStomp.deactivate();
          }
        }),
      );
    });
  }
}