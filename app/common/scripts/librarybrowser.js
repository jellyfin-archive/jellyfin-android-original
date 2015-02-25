(function (globalScope) {

    globalScope.LibraryBrowser = function () {

        var defaultBackground = "#333";

        var self = {};

        self.mapItemsForRepeat = function (items, options, apiClient) {

            for (var i = 0, length = items.length; i < length; i++) {

                var item = items[i];
                item.options = options;
                item.apiClient = apiClient;
                item.listIndex = i;
            }

            return items;
        };

        self.mapItemForRepeat = function () {


        };

        self.getCardItemHtml = function (item, options, apiClient) {

            var html = '';
            var imgUrl = null;
            var background = null;
            var width = null;
            var height = null;

            var forceName = false;

            var downloadHeight = 576;

            var primaryImageAspectRatio = self.getAveragePrimaryImageAspectRatio([item]);

            if (options.autoThumb && item.ImageTags && item.ImageTags.Primary && item.PrimaryImageAspectRatio && item.PrimaryImageAspectRatio >= 1.5) {

                height = 400;
                width = primaryImageAspectRatio ? Math.round(height * primaryImageAspectRatio) : null;

                imgUrl = apiClient.getImageUrl(item.Id, {
                    type: "Primary",
                    height: height,
                    width: width,
                    tag: item.ImageTags.Primary
                });

            } else if (options.autoThumb && item.ImageTags && item.ImageTags.Thumb) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Thumb",
                    maxWidth: downloadHeight,
                    tag: item.ImageTags.Thumb
                });

            } else if (options.preferBackdrop && item.BackdropImageTags && item.BackdropImageTags.length) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Backdrop",
                    maxWidth: downloadHeight,
                    tag: item.BackdropImageTags[0]
                });

            } else if (options.preferThumb && item.ImageTags && item.ImageTags.Thumb) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Thumb",
                    maxWidth: downloadHeight,
                    tag: item.ImageTags.Thumb
                });

            } else if (options.preferBanner && item.ImageTags && item.ImageTags.Banner) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Banner",
                    maxWidth: 700,
                    tag: item.ImageTags.Banner
                });

            } else if (options.preferThumb && item.SeriesThumbImageTag && options.inheritThumb !== false) {

                imgUrl = apiClient.getScaledImageUrl(item.SeriesId, {
                    type: "Thumb",
                    maxWidth: downloadHeight,
                    tag: item.SeriesThumbImageTag
                });

            } else if (options.preferThumb && item.ParentThumbItemId && options.inheritThumb !== false) {

                imgUrl = apiClient.getThumbImageUrl(item.ParentThumbItemId, {
                    type: "Thumb",
                    maxWidth: downloadHeight
                });

            } else if (options.preferThumb && item.BackdropImageTags && item.BackdropImageTags.length) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Backdrop",
                    maxWidth: downloadHeight,
                    tag: item.BackdropImageTags[0]
                });

                forceName = true;

            } else if (item.ImageTags && item.ImageTags.Primary) {

                height = 400;
                width = primaryImageAspectRatio ? Math.round(height * primaryImageAspectRatio) : null;

                imgUrl = apiClient.getImageUrl(item.Id, {
                    type: "Primary",
                    height: height,
                    width: width,
                    tag: item.ImageTags.Primary
                });

            }
            else if (item.ParentPrimaryImageTag) {

                height = 400;

                imgUrl = apiClient.getImageUrl(item.ParentPrimaryImageItemId, {
                    type: "Primary",
                    height: height,
                    tag: item.ParentPrimaryImageTag
                });
            }
            else if (item.AlbumId && item.AlbumPrimaryImageTag) {

                height = 220;
                width = primaryImageAspectRatio ? Math.round(height * primaryImageAspectRatio) : null;

                imgUrl = apiClient.getScaledImageUrl(item.AlbumId, {
                    type: "Primary",
                    height: height,
                    width: width,
                    tag: item.AlbumPrimaryImageTag
                });

            }
            else if (item.Type == 'Season' && item.ImageTags && item.ImageTags.Thumb) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Thumb",
                    maxWidth: downloadHeight,
                    tag: item.ImageTags.Thumb
                });

            }
            else if (item.BackdropImageTags && item.BackdropImageTags.length) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Backdrop",
                    maxWidth: downloadHeight,
                    tag: item.BackdropImageTags[0]
                });

            } else if (item.ImageTags && item.ImageTags.Thumb) {

                imgUrl = apiClient.getScaledImageUrl(item.Id, {
                    type: "Thumb",
                    maxWidth: downloadHeight,
                    tag: item.ImageTags.Thumb
                });

            } else if (item.SeriesThumbImageTag) {

                imgUrl = apiClient.getScaledImageUrl(item.SeriesId, {
                    type: "Thumb",
                    maxWidth: downloadHeight,
                    tag: item.SeriesThumbImageTag
                });

            } else if (item.ParentThumbItemId) {

                imgUrl = apiClient.getThumbImageUrl(item, {
                    type: "Thumb",
                    maxWidth: downloadHeight
                });

            } else {
                if (item.Name && options.showTitle) {
                    imgUrl = 'css/images/items/list/collection.png';
                }
                background = defaultBackground;
            }

            var cssClass = "cardItem";

            if (options.transparent !== false) {
                cssClass += " transparentCard";
            }

            cssClass += ' ' + options.shape + 'Card';

            var mediaSourceCount = item.MediaSourceCount || 1;

            var href = '#';

            if (item.UserData) {
                //cssClass += ' ' + self.getUserDataCssClass(item.UserData.Key);
            }

            if (options.showChildCountIndicator && item.ChildCount) {
                cssClass += ' groupedCard';

                if (item.Type == 'Series') {
                    cssClass += ' unplayedGroupings';
                }
            }

            if (options.showTitle && !options.overlayText) {
                cssClass += ' bottomPaddedCard';
            }

            var dataAttributes = self.getItemDataAttributes(item, options, item.listIndex);

            var defaultActionAttribute = options.defaultAction ? (' data-action="' + options.defaultAction + '"') : '';

            // card
            html += '<div' + dataAttributes + ' class="' + cssClass + '">';

            var style = "";

            if (imgUrl && !options.lazy) {
                style += 'background-image:url(\'' + imgUrl + '\');';
            }

            if (background) {
                style += "background-color:" + background + ";";
            }

            var imageCssClass = 'cardImage';
            if (options.coverImage) {
                imageCssClass += " coveredCardImage";
            }
            if (options.centerImage) {
                imageCssClass += " centeredCardImage";
            }

            var dataSrc = "";

            if (options.lazy) {
                imageCssClass += " lazy";
                dataSrc = ' data-src="' + imgUrl + '"';
            }

            var cardboxCssClass = 'cardBox';

            if (options.cardLayout) {
                cardboxCssClass += ' visualCardBox';
            }
            html += '<div class="' + cardboxCssClass + '">';
            html += '<div class="cardScalable">';

            html += '<div class="cardPadder"></div>';

            var anchorCssClass = "cardContent";

            if (options.defaultAction) {
                anchorCssClass += ' itemWithAction';
            }

            html += '<a class="' + anchorCssClass + '" href="' + href + '"' + defaultActionAttribute + '>';
            html += '<div class="' + imageCssClass + '" style="' + style + '"' + dataSrc + '></div>';

            html += '<div class="cardOverlayTarget"></div>';

            if (item.LocationType == "Offline" || item.LocationType == "Virtual") {
                if (options.showLocationTypeIndicator !== false) {
                    //html += LibraryBrowser.getOfflineIndicatorHtml(item);
                }
            } else if (options.showUnplayedIndicator !== false) {
                //html += LibraryBrowser.getPlayedIndicatorHtml(item);
            } else if (options.showChildCountIndicator) {
                //html += LibraryBrowser.getGroupCountIndicator(item);
            }

            if (mediaSourceCount > 1) {
                html += '<div class="mediaSourceIndicator">' + mediaSourceCount + '</div>';
            }
            if (item.IsUnidentified) {
                html += '<div class="unidentifiedIndicator"><div class="ui-icon-alert ui-btn-icon-notext"></div></div>';
            }

            var progressHtml = options.showProgress === false || item.IsFolder ? '' : self.getItemProgressBarHtml((item.Type == 'Recording' ? item : item.UserData));

            var footerOverlayed = false;

            if (options.overlayText || (forceName && !options.showTitle)) {
                html += self.getCardFooterText(item, options, imgUrl, forceName, 'cardFooter', progressHtml);
                footerOverlayed = true;
            }
            else if (progressHtml) {
                html += '<div class="cardFooter">';
                html += "<div class='cardProgress cardText'>";
                html += progressHtml;
                html += "</div>";
                // cardFooter
                html += "</div>";

                progressHtml = '';
            }

            // cardContent
            html += '</a>';

            // cardScalable
            html += '</div>';

            if (!options.overlayText && !footerOverlayed) {
                html += self.getCardFooterText(item, options, imgUrl, forceName, 'cardFooter outerCardFooter', progressHtml);
            }

            // cardBox
            html += '</div>';

            // card
            html += "</div>";

            return html;
        };

        self.getCardFooterText = function (item, options, imgUrl, forceName, footerClass, progressHtml) {

            var html = '';

            html += '<div class="' + footerClass + '">';

            if (options.cardLayout) {
                html += '<div class="cardText" style="text-align:right; float:right;">';
                // Render out the jqm classes so that we don't have to call trigger create
                html += '<button class="listviewMenuButton ui-btn ui-icon-ellipsis-v ui-btn-icon-notext ui-btn-inline ui-shadow ui-corner-all" type="button" data-inline="true" data-iconpos="notext" data-icon="ellipsis-v" style="margin: 4px 0 0;"></button>';
                html += "</div>";
            }

            var name = self.getPosterViewDisplayName(item, options.displayAsSpecial);

            if (!imgUrl && !options.showTitle) {
                html += "<div class='cardDefaultText'>";
                html += htmlEncode(name);
                html += "</div>";
            }

            var cssClass = options.centerText ? "cardText cardTextCentered" : "cardText";

            var lines = [];

            if (options.showParentTitle) {

                lines.push(item.EpisodeTitle ? item.Name : (item.SeriesName || item.Album || item.AlbumArtist || item.GameSystem || ""));
            }

            if (options.showTitle || forceName) {

                lines.push(htmlEncode(name));
            }

            if (options.showItemCounts) {

                var itemCountHtml = LibraryBrowser.getItemCountsHtml(options, item);

                lines.push(itemCountHtml);
            }

            if (options.textLines) {
                var additionalLines = options.textLines(item);
                for (var i = 0, length = additionalLines.length; i < length; i++) {
                    lines.push(additionalLines[i]);
                }
            }

            if (options.showSongCount) {

                var songLine = '';

                if (item.SongCount) {
                    songLine = item.SongCount == 1 ?
					Globalize.translate('ValueOneSong') :
					Globalize.translate('ValueSongCount', item.SongCount);
                }

                lines.push(songLine);
            }

            if (options.showPremiereDate && item.PremiereDate) {

                try {

                    lines.push(LibraryBrowser.getPremiereDateText(item));

                } catch (err) {
                    lines.push('');

                }
            }

            if (options.showYear) {

                lines.push(item.ProductionYear || '');
            }

            if (options.showSeriesYear) {

                if (item.Status == "Continuing") {

                    lines.push(Globalize.translate('ValueSeriesYearToPresent', item.ProductionYear || ''));

                } else {
                    lines.push(item.ProductionYear || '');
                }

            }

            html += self.getCardTextLines(lines, cssClass, !options.overlayText);

            if (options.overlayText) {

                if (progressHtml) {
                    html += "<div class='cardText cardProgress'>";
                    html += progressHtml;
                    html += "</div>";
                }
            }

            //cardFooter
            html += "</div>";
            return html;
        };

        self.getCardTextLines = function (lines, cssClass, forceLines) {

            var html = '';

            var valid = 0;
            var i, length;

            for (i = 0, length = lines.length; i < length; i++) {

                var text = lines[i];

                if (text) {
                    html += "<div class='" + cssClass + "'>";
                    html += text;
                    html += "</div>";
                    valid++;
                }
            }

            if (forceLines) {
                while (valid < length) {
                    html += "<div class='" + cssClass + "'>&nbsp;</div>";
                    valid++;
                }
            }

            return html;
        };

        self.getPosterViewDisplayName = function (item, displayAsSpecial, includeParentInfo) {

            if (!item) {
                throw new Error("null item passed into getPosterViewDisplayName");
            }

            var name = item.EpisodeTitle || item.Name;

            if (item.Type == "TvChannel") {

                if (item.Number) {
                    return item.Number + ' ' + name;
                }
                return name;
            }
            if (displayAsSpecial && item.Type == "Episode" && item.ParentIndexNumber == 0) {

                name = Globalize.translate('ValueSpecialEpisodeName', name);

            } else if (item.Type == "Episode" && item.IndexNumber != null && item.ParentIndexNumber != null) {

                var displayIndexNumber = item.IndexNumber;

                var number = "E" + displayIndexNumber;

                if (includeParentInfo !== false) {
                    number = "S" + item.ParentIndexNumber + ", " + number;
                }

                if (item.IndexNumberEnd) {

                    displayIndexNumber = item.IndexNumberEnd;
                    number += "-" + displayIndexNumber;
                }

                name = number + " - " + name;

            }

            return name;
        };

        self.getItemProgressBarHtml = function (item) {

            if (item.Type == "Recording" && item.CompletionPercentage) {

                return '<progress class="itemProgressBar recordingProgressBar" min="0" max="100" value="' + item.CompletionPercentage + '"></progress>';
            }

            var pct = item.PlayedPercentage;

            if (pct && pct < 100) {

                return '<progress class="itemProgressBar" min="0" max="100" value="' + pct + '"></progress>';
            }

            return null;
        };

        self.getItemDataAttributes = function (item, options, index) {

            var atts = [];

            var itemCommands = self.getItemCommands(item, options);

            atts.push('data-itemid="' + item.Id + '"');
            atts.push('data-commands="' + itemCommands.join(',') + '"');
            atts.push('data-context="' + (options.context || '') + '"');
            atts.push('data-itemtype="' + item.Type + '"');
            atts.push('data-mediatype="' + (item.MediaType || '') + '"');
            atts.push('data-positionticks="' + (item.UserData.PlaybackPositionTicks || 0) + '"');

            atts.push('data-playaccess="' + (item.PlayAccess || '') + '"');
            atts.push('data-locationtype="' + (item.LocationType || '') + '"');
            atts.push('data-index="' + index + '"');

            if (item.IsPlaceHolder) {
                atts.push('data-placeholder="true"');
            }

            var html = atts.join(' ');

            if (html) {
                html = ' ' + html;
            }

            return html;
        };

        self.getItemCommands = function (item, options) {

            var itemCommands = [];

            //if (MediaController.canPlay(item)) {
            //    itemCommands.push('playmenu');
            //}

            if (item.Type != "Recording" && item.Type != "Program") {
                itemCommands.push('edit');
            }

            if (item.LocalTrailerCount) {
                itemCommands.push('trailer');
            }

            if (item.MediaType == "Audio" || item.Type == "MusicAlbum" || item.Type == "MusicArtist" || item.Type == "MusicGenre") {
                itemCommands.push('instantmix');
            }

            if (item.IsFolder || item.Type == "MusicArtist" || item.Type == "MusicGenre") {
                itemCommands.push('shuffle');
            }

            //if (PlaylistManager.supportsPlaylists(item)) {

            //    if (options.showRemoveFromPlaylist) {
            //        itemCommands.push('removefromplaylist');
            //    } else {
            //        itemCommands.push('playlist');
            //    }
            //}

            //if (BoxSetEditor.supportsAddingToCollection(item)) {
            //    itemCommands.push('addtocollection');
            //}

            if (options.playFromHere) {
                itemCommands.push('playfromhere');
                itemCommands.push('queuefromhere');
            }

            // There's no detail page with a dedicated delete function
            if (item.Type == 'Playlist' || item.Type == 'BoxSet') {

                if (item.CanDelete) {
                    itemCommands.push('delete');
                }
            }

            //if (SyncManager.isAvailable(item)) {
            //    itemCommands.push('sync');
            //}

            return itemCommands;
        };

        self.getAveragePrimaryImageAspectRatio = function (items) {

            var values = [];

            for (var i = 0, length = items.length; i < length; i++) {

                var ratio = items[i].PrimaryImageAspectRatio || 0;

                if (!ratio) {
                    continue;
                }

                values[values.length] = ratio;
            }

            if (!values.length) {
                return null;
            }

            // Use the median
            values.sort(function (a, b) { return a - b; });

            var half = Math.floor(values.length / 2);

            var result;

            if (values.length % 2)
                result = values[half];
            else
                result = (values[half - 1] + values[half]) / 2.0;

            // If really close to 2:3 (poster image), just return 2:3
            if (Math.abs(0.66666666667 - result) <= .15) {
                return 0.66666666667;
            }

            // If really close to 16:9 (episode image), just return 16:9
            if (Math.abs(1.777777778 - result) <= .2) {
                return 1.777777778;
            }

            // If really close to 1 (square image), just return 1
            if (Math.abs(1 - result) <= .15) {
                return 1;
            }

            // If really close to 4:3 (poster image), just return 2:3
            if (Math.abs(1.33333333333 - result) <= .15) {
                return 1.33333333333;
            }

            return result;
        };

        return self;
    }();

})(this);