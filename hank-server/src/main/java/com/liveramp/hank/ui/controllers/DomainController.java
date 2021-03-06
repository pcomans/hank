/**
 *  Copyright 2011 LiveRamp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.liveramp.hank.ui.controllers;

import com.liveramp.hank.coordinator.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DomainController extends Controller {

  private final Coordinator coordinator;

  public DomainController(String name, Coordinator coordinator) {
    super(name);
    this.coordinator = coordinator;
    actions.put("create", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String domainName = req.getParameter("name");
        int numParts = Integer.parseInt(req.getParameter("numParts"));

        String storageEngineFactoryName = req.getParameter("storageEngineFactorySelect");
        if (storageEngineFactoryName.equals("__other__")) {
          storageEngineFactoryName = req.getParameter("storageEngineFactoryName");
        }

        String storageEngineOptions = req.getParameter("storageEngineOptions");
        final String requiredHostFlags = req.getParameter("requiredHostFlags");

        String partitionerName = req.getParameter("partitionerSelect");
        if (partitionerName.equals("__other__")) {
          partitionerName = req.getParameter("partitionerOther");
        }
        DomainController.this.coordinator.addDomain(domainName, numParts, storageEngineFactoryName,
            storageEngineOptions, partitionerName, Hosts.splitHostFlags(requiredHostFlags));
        redirect("/domains.jsp", resp);
      }
    });
    actions.put("delete", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doDeleteDomain(req, resp);
      }
    });
    actions.put("defunctify", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain domain = DomainController.this.coordinator.getDomain(req.getParameter("n"));
        final DomainVersion domainVersion = domain.getVersion(Integer.parseInt(req.getParameter("ver")));
        domainVersion.setDefunct(true);
        redirect("/domain.jsp?n=" + req.getParameter("n"), resp);
      }
    });
    actions.put("undefunctify", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain domain = DomainController.this.coordinator.getDomain(req.getParameter("n"));
        final DomainVersion domainVersion = domain.getVersion(Integer.parseInt(req.getParameter("ver")));
        domainVersion.setDefunct(false);
        redirect("/domain.jsp?n=" + req.getParameter("n"), resp);
      }
    });
    actions.put("close", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain domain = DomainController.this.coordinator.getDomain(req.getParameter("n"));
        final DomainVersion domainVersion = domain.getVersion(Integer.parseInt(req.getParameter("ver")));
        domainVersion.close();
        redirect("/domain.jsp?n=" + req.getParameter("n"), resp);
      }
    });
    actions.put("delete_version", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain domain = DomainController.this.coordinator.getDomain(req.getParameter("n"));
        final Integer domainVersion = Integer.parseInt(req.getParameter("ver"));
        domain.deleteVersion(domainVersion);
        redirect("/domain.jsp?n=" + req.getParameter("n"), resp);
      }
    });
    actions.put("delete_all_defunct_versions", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain domain = DomainController.this.coordinator.getDomain(req.getParameter("n"));
        for (DomainVersion domainVersion : domain.getVersions()) {
          if (domainVersion.isDefunct()) {
            domain.deleteVersion(domainVersion.getVersionNumber());
          }
        }
        redirect("/domain.jsp?n=" + req.getParameter("n"), resp);
      }
    });
    actions.put("update", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doUpdateDomain(req, resp);
      }
    });
    actions.put("cleanup", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain domain = DomainController.this.coordinator.getDomain(req.getParameter("n"));
        domain.getStorageEngine().getRemoteDomainVersionDeleter().deleteVersion(Integer.parseInt(req.getParameter("ver")));
        final DomainVersion domainVersion = domain.getVersion(Integer.parseInt(req.getParameter("ver")));
        domainVersion.setDefunct(true);
        redirect("/domain.jsp?n=" + req.getParameter("n"), resp);
      }
    });
    actions.put("clean_domains", new Action() {
      @Override
      protected void action(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doCleanDomains();
        redirect("/domains.jsp", resp);
      }
    });
  }

  private void doCleanDomains() throws IOException {
    Domains.cleanDomains(coordinator.getDomains());
  }

  private void doDeleteDomain(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final Domain domain = coordinator.getDomain(req.getParameter("name"));
    boolean isInUse = false;
    // check if this domain is in use anywhere
    for (RingGroup rg : coordinator.getRingGroups()) {
      DomainGroup dg = rg.getDomainGroup();
      if (dg.getDomains().contains(domain)) {
        isInUse = true;
        break;
      }
    }
    if (!isInUse) {
      coordinator.deleteDomain(domain.getName());
    }
    resp.sendRedirect("/domains.jsp");
  }

  private void doUpdateDomain(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final String domainName = req.getParameter("name");
    final String partitionerClassName = req.getParameter("partitionerClassName");
    final String requiredHostFlags = req.getParameter("requiredHostFlags");
    final String storageEngineFactoryClassName = req.getParameter("storageEngineFactoryClassName");
    final String storageEngineOptions = req.getParameter("storageEngineOptions");
    final Domain domain = coordinator.getDomain(domainName);
    if (domain == null) {
      throw new IOException("Could not get Domain '" + domainName + "' from Configurator.");
    } else {
      coordinator.updateDomain(domainName,
          domain.getNumParts(),
          storageEngineFactoryClassName,
          storageEngineOptions,
          partitionerClassName,
          Hosts.splitHostFlags(requiredHostFlags));
    }
    resp.sendRedirect("/domains.jsp");
  }
}
