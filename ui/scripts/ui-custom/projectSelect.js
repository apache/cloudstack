// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

(function($, cloudStack) {
    $(window).bind('cloudStack.ready', function() {
        var $header = $('#header .controls');
        var $projectSwitcher = $('<div>').addClass('project-switcher');
        var $projectSelect = $('<select>').append(
            $('<option>').attr('value', '-1').html(_l('Default view'))
        );
        var $label = $('<label>').html('Project:');

        // Get project list
        cloudStack.projects.dataProvider({
            context: cloudStack.context,
            response: {
                success: function(args) {
                    var projects = args.data;
                    var arrayOfProjs = [];

                    $(projects).map(function(index, project) {
                        var proj = {id: _s(project.id), html: _s(project.displaytext ? project.displaytext : project.name)};
                        arrayOfProjs.push(proj);
                    });

                    arrayOfProjs.sort(function(a,b) {
                        return a.html.localeCompare(b.html);
                    });

                    $(arrayOfProjs).map(function(index, project) {
                        var $option = $('<option>').val(_s(project.id));

                        $option.html(_s(project.html));
                        $option.appendTo($projectSelect);
                    });
                },
                error: function() {}
            }
        });

        $projectSwitcher.append($label, $projectSelect);
        $projectSwitcher.insertBefore($header.find('.region-switcher'));

        // Change project event
        $projectSelect.change(function() {
            var projectID = $projectSelect.val();

            if (projectID != -1) {
                cloudStack.context.projects = [{
                    id: projectID
                }];

                cloudStack.uiCustom.projects({
                    alreadySelected: true
                });
            } else {
                cloudStack.context.projects = null;
                $('#cloudStack3-container').removeClass('project-view');
                $('#navigation li.dashboard').click();
            }
        });
    });
}(jQuery, cloudStack));
