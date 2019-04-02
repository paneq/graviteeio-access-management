/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Component, OnInit, ViewChild} from '@angular/core';
import { ActivatedRoute, Router } from "@angular/router";
import { AppConfig } from "../../../../config/app.config";
import { AuditService } from "../../../services/audit.service";
import * as moment from 'moment';
import {PlatformService} from "../../../services/platform.service";

@Component({
  selector: 'app-audits',
  templateUrl: './audits.component.html',
  styleUrls: ['./audits.component.scss']
})
export class AuditsComponent implements OnInit {
  @ViewChild('auditsTable') table: any;
  audits: any[];
  pagedAudits: any;
  domainId: string;
  page: any = {};
  eventTypes: string[];
  eventType: string;
  eventStatus: string;
  startDate: any;
  endDate: any;
  displayReset: boolean = false;
  expanded: any = {};
  config: any = {lineWrapping:true, lineNumbers: true, readOnly: true, mode: 'application/json'};
  private startDateChanged: boolean = false;
  private endDateChanged: boolean = false;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private auditService: AuditService,
              private platformService: PlatformService) {
    this.page.pageNumber = 0;
    this.page.size = 10;
  }

  ngOnInit() {
    this.domainId = this.route.snapshot.parent.parent.params['domainId'];
    if (this.router.routerState.snapshot.url.startsWith('/settings')) {
      this.domainId = AppConfig.settings.authentication.domainId;
    }

    this.pagedAudits = this.route.snapshot.data['audits'];
    this.audits = this.pagedAudits.data;
    this.page.totalElements = this.pagedAudits.totalCount;

    // load event types
    this.platformService.auditEventTypes().map(res => res.json()).subscribe(data => this.eventTypes = data);
  }


  loadAudits() {
    this.auditService.findByDomain(this.domainId, this.page.pageNumber, this.page.size).map(res => res.json()).subscribe(pagedAudits => {
      this.page.totalElements = pagedAudits.totalCount;
      this.audits = pagedAudits.data;
    });
  }

  get isEmpty() {
    return !this.audits || this.audits.length == 0 && !this.displayReset;
  }

  setPage(pageInfo){
    this.page.pageNumber = pageInfo.offset;
    this.loadAudits();
  }

  isUnknownActor(row) {
    return row.outcome.status === 'FAILURE' && row.type === 'USER_LOGIN';
  }

  getActorUrl(row) {
    let routerLink = [];

    if (row.domain === AppConfig.settings.authentication.domainId || row.accessPoint.id === AppConfig.settings.authentication.domainId) {
      routerLink.push('/settings');
      routerLink.push('management');
    } else {
      routerLink.push('/domains');
      routerLink.push(row.domain);
      routerLink.push('settings');
    }

    routerLink.push('users');
    routerLink.push(row.actor.id);

    return routerLink;
  }

  getTargetUrl(row) {
    let routerLink = [];
    routerLink.push('/domains');
    routerLink.push(row.target.domain);
    if (row.target.type !== 'CLIENT') {
      routerLink.push('settings');
    }
    if (row.target.type !== 'DOMAIN') {
      if (row.target.type !== 'IDENTITY_PROVIDER') {
        routerLink.push(row.target.type.toLowerCase() + 's');
      } else {
        routerLink.push('providers');
      }
      if (row.target.type === 'FORM' || row.target.type === 'EMAIL') {
        routerLink.push(row.target.type.toLowerCase());
      } else {
        routerLink.push(row.target.id);
      }
    }
    return routerLink;
  }

  getTargetParams(row) {
    let params = {};
    if (row.target.type === 'FORM' || row.target.type === 'EMAIL') {
      params['template'] = row.target.displayName.toUpperCase();
    }
    return params;
  }

  search(event) {
    event.preventDefault();
    this.searchAudits();
  }

  startDateChange(element, event) {
    this.startDateChanged = true;
    this.updateForm(element, event);
  }

  endDateChange(element, event) {
    this.endDateChanged = true;
    this.updateForm(element, event);
  }

  updateForm(element, event) {
    this.displayReset = true;
  }

  resetForm() {
    this.eventType = null;
    this.startDate = null;
    this.endDate = null;
    this.displayReset = false;
    this.loadAudits();
  }

  refresh() {
    if (this.displayReset) {
      this.searchAudits();
    } else {
      this.loadAudits();
    }
  }

  searchAudits() {
    let from = this.startDateChanged ? moment(this.startDate).valueOf() : null;
    let to = this.endDateChanged ? moment(this.endDate).valueOf() : null;
    this.auditService.search(this.domainId, this.page.pageNumber, this.page.size, this.eventType, this.eventStatus, from, to).map(res => res.json()).subscribe(pagedAudits => {
      this.page.totalElements = pagedAudits.totalCount;
      this.audits = pagedAudits.data;
    });
  }

  toggleExpandRow(row) {
    this.table.rowDetail.toggleExpandRow(row);
  }

  auditDetails(row) {
    if (row.outcome.message) {
      if (row.outcome.status === 'SUCCESS') {
        return JSON.stringify(JSON.parse(row.outcome.message), null, '  ');
      } else {
        return row.outcome.message;
      }
    } else {
      return row.type + ' success';
    }
  }
}
