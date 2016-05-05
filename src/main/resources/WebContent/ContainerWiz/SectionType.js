(function () {
	var boolCast = /^(?:1|true|yes|t|y)$/;
	ContainerWiz.SectionType = RSuite.CacheableObject
		.extend(Ember.ArrayProxy.PrototypeMixin, {
			content: null,
			init: function () {
				// Original impl (content: []) was not multi-instance safe.
				if (!this.get('content')) {
					this.set('content', []);
				}
			}
		})
		.reopenClass({
			_requestSectionType: function (config, subPage) {
				return RSuite.services({
					service: "api/rsuite-container-wizard-plugin-ws-get-section-type-info",
					data: {
						confAlias: config,
						nextSubPageIdx: subPage
					}
				});
			},

			SectionTypeTemplateMixin: Ember.Mixin.create({
				// Will defer load until requested by the view
				xmlTemplates: function () {
					var value = [Ember.Object.create({ name: 'Loading...', managedObject: null })];
					RSuite.services({
							service: 'api/rsuite-container-wizard-plugin-ws-get-template-info',
							data: {
								xmlTemplateType: this.get('xmlTemplateType')
							}
						}).done(function (templates) {
							value.replace(0, value.length, templates);
						}).fail(function (xhr, status, error) {
							value[0].set('name', "Failed to load template: " + error ? (error.message || error) : status);
						});
					return value;
				}.property('xmlTemplateType')
			}),
			request: function (config) {
				var inst = this.getCached(config, false),
					Self = this,
					subPage;
				config = config.split(':');
				if (config.length > 1 && !isNaN(config[config.length - 1])) {
					subPage = config.pop();
					config = config.join(':');
				} else {
					subPage = 0;
					config = config[0];
				}
				inst.setProperties({ confAlias: config, nextSubPageIndex: subPage });
				var delegate = new $.Deferred();
				this._requestSectionType(config, subPage)
					.fail(delegate.reject.bind(delegate))
					.done(function (content) {
						var oldContent = inst.get('content'),
							newContent = [];
						content.forEach(function (item) {
							item.mayRepeat = boolCast.test(item.mayRepeat);
							item.isRequired = boolCast.test(item.isRequired);
							item.reopen(Self.SectionTypeTemplateMixin);
							newContent.push(item);
						});
						oldContent.replace(0, oldContent.length, newContent);
						// Instead of replacing `content`, we update the array that's there.
						delegate.resolve({});
					});
				// Cleaner than new $.Deferred / def.resolve / def.reject.
				return delegate;
			}
		});
}());
